package com.webhookplatform.service;

import com.webhookplatform.entity.*;
import com.webhookplatform.kafka.WebhookEventMessage;
import com.webhookplatform.repository.DeliveryAttemptRepository;
import com.webhookplatform.repository.DestinationRepository;
import com.webhookplatform.repository.WebhookEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/** Delivers a stored webhook event to every active destination registered for its source. */
@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final WebhookEventRepository eventRepository;
    private final DestinationRepository destinationRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final RetryStateService retryStateService;
    private final MeterRegistry meterRegistry;
    private final RestClient restClient;
    private final int maxAttempts;

    public DeliveryService(WebhookEventRepository eventRepository,
                           DestinationRepository destinationRepository,
                           DeliveryAttemptRepository attemptRepository,
                           RetryStateService retryStateService,
                           MeterRegistry meterRegistry,
                           RestClient.Builder restClientBuilder,
                           @Value("${app.delivery.max-attempts:5}") int maxAttempts) {
        this.eventRepository = eventRepository;
        this.destinationRepository = destinationRepository;
        this.attemptRepository = attemptRepository;
        this.retryStateService = retryStateService;
        this.meterRegistry = meterRegistry;
        this.restClient = restClientBuilder.build();
        this.maxAttempts = maxAttempts;
    }

    public void deliver(WebhookEventMessage message) {
        Optional<WebhookEvent> maybeEvent = eventRepository.findById(message.id());
        if (maybeEvent.isEmpty()) {
            log.warn("Event {} no longer exists, skipping delivery", message.id());
            return;
        }
        WebhookEvent event = maybeEvent.get();

        List<Destination> destinations =
                destinationRepository.findBySourceIgnoreCaseAndActiveTrue(event.getSource());

        if (destinations.isEmpty()) {
            event.setStatus(EventStatus.NO_DESTINATION);
            eventRepository.save(event);
            log.info("No active destination for source={} (event {})", event.getSource(), event.getId());
            return;
        }

        event.setStatus(EventStatus.PROCESSING);
        eventRepository.save(event);

        boolean allDelivered = true;
        boolean anyDelivered = false;

        for (Destination destination : destinations) {
            boolean ok = attemptDelivery(event, destination, message.attempt());
            allDelivered &= ok;
            anyDelivered |= ok;
        }

        if (allDelivered) {
            event.setStatus(EventStatus.DELIVERED);
        } else if (message.attempt() >= maxAttempts) {
            event.setStatus(anyDelivered ? EventStatus.PARTIALLY_DELIVERED : EventStatus.FAILED);
        } else {
            event.setStatus(EventStatus.PROCESSING);
            retryStateService.scheduleRetry(message);
        }
        eventRepository.save(event);
    }

    private boolean attemptDelivery(WebhookEvent event, Destination destination, int attemptNumber) {
        Timer.Sample sample = Timer.start(meterRegistry);
        long start = System.currentTimeMillis();

        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setEventId(event.getId());
        attempt.setDestinationId(destination.getId());
        attempt.setAttemptNumber(attemptNumber);

        try {
            var response = restClient.post()
                    .uri(destination.getTargetUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Source", event.getSource())
                    .header("X-Webhook-Event-Id", event.getEventId())
                    .header("X-Webhook-Event-Type", event.getEventType())
                    .header("X-Webhook-Attempt", String.valueOf(attemptNumber))
                    .headers(h -> signature(destination, event).ifPresent(sig ->
                            h.add("X-Webhook-Signature", sig)))
                    .body(event.getPayload())
                    .retrieve()
                    .toEntity(String.class);

            int status = response.getStatusCode().value();
            attempt.setResponseStatus(status);
            attempt.setResponseBody(truncate(response.getBody()));
            boolean success = status >= 200 && status < 300;
            attempt.setStatus(success ? DeliveryStatus.SUCCESS
                    : (attemptNumber >= maxAttempts ? DeliveryStatus.FAILED : DeliveryStatus.RETRY_SCHEDULED));
            if (!success) {
                attempt.setErrorMessage("Non-2xx response: " + status);
            }
            record(destination, success, attemptNumber);
            return success;
        } catch (Exception e) {
            attempt.setStatus(attemptNumber >= maxAttempts ? DeliveryStatus.FAILED : DeliveryStatus.RETRY_SCHEDULED);
            attempt.setErrorMessage(truncate(e.getMessage()));
            record(destination, false, attemptNumber);
            log.warn("Delivery failed event={} destination={} attempt={}: {}",
                    event.getId(), destination.getId(), attemptNumber, e.getMessage());
            return false;
        } finally {
            attempt.setDurationMs(System.currentTimeMillis() - start);
            attemptRepository.save(attempt);
            sample.stop(Timer.builder("webhook_delivery_duration")
                    .tag("destination", String.valueOf(destination.getId()))
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    private void record(Destination destination, boolean success, int attemptNumber) {
        Counter.builder(success ? "webhook_deliveries_success_total" : "webhook_deliveries_failed_total")
                .tag("source", destination.getSource())
                .register(meterRegistry)
                .increment();
        if (attemptNumber > 1) {
            Counter.builder("webhook_delivery_retries_total")
                    .tag("source", destination.getSource())
                    .register(meterRegistry)
                    .increment();
        }
    }

    private Optional<String> signature(Destination destination, WebhookEvent event) {
        if (destination.getSecret() == null || destination.getSecret().isBlank()) {
            return Optional.empty();
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(destination.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(event.getPayload().getBytes(StandardCharsets.UTF_8));
            return Optional.of("sha256=" + HexFormat.of().formatHex(digest));
        } catch (Exception e) {
            log.error("Unable to sign payload for destination {}", destination.getId(), e);
            return Optional.empty();
        }
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() > 2000 ? value.substring(0, 2000) : value;
    }
}
