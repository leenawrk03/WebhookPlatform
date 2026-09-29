package com.webhookplatform.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookplatform.kafka.WebhookEventMessage;
import com.webhookplatform.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes webhook events from Kafka and delivers them to registered destinations. */
@Component
public class DeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);

    private final ObjectMapper objectMapper;
    private final DeliveryService deliveryService;

    public DeliveryWorker(ObjectMapper objectMapper, DeliveryService deliveryService) {
        this.objectMapper = objectMapper;
        this.deliveryService = deliveryService;
    }

    @KafkaListener(topics = "${app.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String payload) {
        try {
            WebhookEventMessage message = objectMapper.readValue(payload, WebhookEventMessage.class);
            deliveryService.deliver(message);
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", payload, e);
        }
    }
}
