package com.webhookplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhookplatform.entity.WebhookEvent;
import com.webhookplatform.kafka.WebhookEventProducer;
import com.webhookplatform.repository.WebhookEventRepository;
import com.webhookplatform.service.WebhookIngestService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebhookIngestServiceTest {

    private WebhookEventRepository repository;
    private WebhookEventProducer producer;
    private WebhookIngestService service;

    @BeforeEach
    void setUp() {
        repository = mock(WebhookEventRepository.class);
        producer = mock(WebhookEventProducer.class);
        service = new WebhookIngestService(repository, producer, new ObjectMapper(), new SimpleMeterRegistry());
    }

    @Test
    void rejectsNonJsonBody() {
        assertThatThrownBy(() -> service.ingest("stripe", "not json", Map.of()))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(producer);
    }

    @Test
    void storesAndPublishesNewEvent() {
        when(repository.findByEventId(any())).thenReturn(Optional.empty());
        when(repository.save(any(WebhookEvent.class))).thenAnswer(inv -> {
            WebhookEvent e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        WebhookEvent event = service.ingest("stripe",
                "{\"id\":\"evt_1\",\"type\":\"payment_intent.succeeded\"}", Map.of());

        assertThat(event.getEventId()).isEqualTo("stripe:evt_1");
        assertThat(event.getEventType()).isEqualTo("payment_intent.succeeded");
        verify(producer).publish(any(WebhookEvent.class));
    }

    @Test
    void ignoresDuplicateEvent() {
        WebhookEvent existing = new WebhookEvent();
        existing.setEventId("stripe:evt_1");
        when(repository.findByEventId("stripe:evt_1")).thenReturn(Optional.of(existing));

        WebhookEvent result = service.ingest("stripe", "{\"id\":\"evt_1\"}", Map.of());

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(producer);
        verify(repository, never()).save(any());
    }
}
