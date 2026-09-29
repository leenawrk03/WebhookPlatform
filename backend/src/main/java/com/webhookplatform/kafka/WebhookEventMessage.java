package com.webhookplatform.kafka;

/** Message published to Kafka for asynchronous delivery processing. */
public record WebhookEventMessage(
        Long id,
        String eventId,
        String source,
        String eventType,
        int attempt
) {
    public WebhookEventMessage {
        if (attempt < 1) {
            attempt = 1;
        }
    }
}
