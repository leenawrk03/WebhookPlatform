package com.webhookplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookplatform.kafka.WebhookEventMessage;
import com.webhookplatform.kafka.WebhookEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

/**
 * Keeps retry state in Redis: failed deliveries land in a sorted set keyed by the
 * timestamp of their next attempt. A scheduler drains due entries back into Kafka.
 */
@Service
public class RetryStateService {

    private static final Logger log = LoggerFactory.getLogger(RetryStateService.class);
    private static final String RETRY_ZSET = "webhook:retries";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final WebhookEventProducer producer;
    private final int maxAttempts;
    private final long baseBackoffSeconds;

    public RetryStateService(StringRedisTemplate redis,
                             ObjectMapper objectMapper,
                             WebhookEventProducer producer,
                             @Value("${app.delivery.max-attempts:5}") int maxAttempts,
                             @Value("${app.delivery.base-backoff-seconds:10}") long baseBackoffSeconds) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.producer = producer;
        this.maxAttempts = maxAttempts;
        this.baseBackoffSeconds = baseBackoffSeconds;
    }

    /** Schedules the next attempt with exponential backoff (10s, 20s, 40s, 80s...). */
    public void scheduleRetry(WebhookEventMessage message) {
        int nextAttempt = message.attempt() + 1;
        if (nextAttempt > maxAttempts) {
            log.warn("Event {} exhausted {} attempts, giving up", message.eventId(), maxAttempts);
            return;
        }
        long delaySeconds = baseBackoffSeconds * (long) Math.pow(2, message.attempt() - 1);
        long dueAt = Instant.now().plusSeconds(delaySeconds).toEpochMilli();

        WebhookEventMessage retry = new WebhookEventMessage(
                message.id(), message.eventId(), message.source(), message.eventType(), nextAttempt);
        try {
            redis.opsForZSet().add(RETRY_ZSET, objectMapper.writeValueAsString(retry), dueAt);
            redis.opsForValue().set("webhook:attempt:" + message.id(), String.valueOf(nextAttempt));
            log.info("Retry {} for event {} scheduled in {}s", nextAttempt, message.eventId(), delaySeconds);
        } catch (Exception e) {
            log.error("Unable to store retry state for event {}", message.eventId(), e);
        }
    }

    /** Re-publishes retries whose backoff window has elapsed. */
    @Scheduled(fixedDelayString = "${app.delivery.retry-poll-ms:5000}")
    public void drainDueRetries() {
        long now = Instant.now().toEpochMilli();
        Set<String> due = redis.opsForZSet().rangeByScore(RETRY_ZSET, 0, now);
        if (due == null || due.isEmpty()) {
            return;
        }
        for (String raw : due) {
            try {
                WebhookEventMessage message = objectMapper.readValue(raw, WebhookEventMessage.class);
                producer.publish(message);
                redis.opsForZSet().remove(RETRY_ZSET, raw);
            } catch (Exception e) {
                log.error("Failed to requeue retry entry {}", raw, e);
            }
        }
    }

    public long pendingRetries() {
        Long size = redis.opsForZSet().zCard(RETRY_ZSET);
        return size == null ? 0 : size;
    }
}
