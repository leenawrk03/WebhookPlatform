package com.webhookplatform.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookplatform.entity.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebhookEventProducer {

    private static final Logger log = LoggerFactory.getLogger(WebhookEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public WebhookEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper,
                                @Value("${app.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(WebhookEvent event) {
        publish(new WebhookEventMessage(event.getId(), event.getEventId(), event.getSource(),
                event.getEventType(), 1));
    }

    public void publish(WebhookEventMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, message.source(), json);
            log.debug("Published event {} to {}", message.eventId(), topic);
        } catch (Exception e) {
            log.error("Failed to publish event {} to Kafka", message.eventId(), e);
            throw new IllegalStateException("Kafka publish failed", e);
        }
    }
}
