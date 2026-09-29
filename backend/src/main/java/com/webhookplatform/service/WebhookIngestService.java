package com.webhookplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookplatform.entity.EventStatus;
import com.webhookplatform.entity.WebhookEvent;
import com.webhookplatform.kafka.WebhookEventProducer;
import com.webhookplatform.repository.WebhookEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates an inbound webhook, stores it in PostgreSQL and publishes it to Kafka.
 */
@Service
public class WebhookIngestService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestService.class);
    private static final int MAX_PAYLOAD_BYTES = 1_000_000;

    private final WebhookEventRepository eventRepository;
    private final WebhookEventProducer producer;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public WebhookIngestService(WebhookEventRepository eventRepository,
                                WebhookEventProducer producer,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.producer = producer;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public WebhookEvent ingest(String source, String rawBody, Map<String, String> headers) {
        validate(source, rawBody);

        String eventId = resolveEventId(source, rawBody, headers);

        Optional<WebhookEvent> existing = eventRepository.findByEventId(eventId);
        if (existing.isPresent()) {
            log.info("Duplicate webhook ignored: source={} eventId={}", source, eventId);
            counter("webhook_events_duplicate_total", source).increment();
            return existing.get();
        }

        WebhookEvent event = new WebhookEvent();
        event.setEventId(eventId);
        event.setSource(source.toLowerCase());
        event.setEventType(resolveEventType(source, rawBody, headers));
        event.setPayload(rawBody);
        event.setHeaders(writeJson(sanitizeHeaders(headers)));
        event.setStatus(EventStatus.RECEIVED);

        WebhookEvent saved = eventRepository.save(event);
        producer.publish(saved);

        counter("webhook_events_received_total", source).increment();
        log.info("Stored webhook event id={} source={} type={}", saved.getId(), saved.getSource(), saved.getEventType());
        return saved;
    }

    private void validate(String source, String rawBody) {
        if (source == null || source.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing source");
        }
        if (rawBody == null || rawBody.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty webhook body");
        }
        if (rawBody.getBytes().length > MAX_PAYLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Payload too large");
        }
        try {
            objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body must be valid JSON");
        }
    }

    /** Providers expose their own event id header/field; fall back to a UUID. */
    private String resolveEventId(String source, String rawBody, Map<String, String> headers) {
        String headerId = firstHeader(headers,
                "x-event-id", "x-github-delivery", "x-razorpay-event-id",
                "x-shopify-webhook-id", "x-gitlab-event-uuid", "x-twilio-idempotency-token");
        if (headerId != null) {
            return source.toLowerCase() + ":" + headerId;
        }
        try {
            var node = objectMapper.readTree(rawBody);
            if (node.hasNonNull("id")) {
                return source.toLowerCase() + ":" + node.get("id").asText();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return source.toLowerCase() + ":" + UUID.randomUUID();
    }

    private String resolveEventType(String source, String rawBody, Map<String, String> headers) {
        String headerType = firstHeader(headers,
                "x-event-type", "x-github-event", "x-gitlab-event", "x-shopify-topic");
        if (headerType != null) {
            return headerType;
        }
        try {
            var node = objectMapper.readTree(rawBody);
            for (String field : new String[]{"type", "event", "event_type", "action"}) {
                if (node.hasNonNull(field)) {
                    return node.get(field).asText();
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "unknown";
    }

    private String firstHeader(Map<String, String> headers, String... names) {
        if (headers == null) return null;
        for (String name : names) {
            String value = headers.get(name.toLowerCase());
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        Map<String, String> copy = new HashMap<>();
        if (headers == null) return copy;
        headers.forEach((k, v) -> {
            String key = k.toLowerCase();
            if (key.equals("authorization") || key.equals("cookie") || key.contains("secret")) {
                copy.put(key, "***redacted***");
            } else {
                copy.put(key, v);
            }
        });
        return copy;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Counter counter(String name, String source) {
        return Counter.builder(name).tag("source", source.toLowerCase()).register(meterRegistry);
    }
}
