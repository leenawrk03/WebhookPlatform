package com.webhookplatform;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.webhookplatform.entity.*;
import com.webhookplatform.kafka.WebhookEventMessage;
import com.webhookplatform.repository.DeliveryAttemptRepository;
import com.webhookplatform.repository.DestinationRepository;
import com.webhookplatform.repository.WebhookEventRepository;
import com.webhookplatform.service.DeliveryService;
import com.webhookplatform.service.RetryStateService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeliveryServiceWireMockTest {

    private WireMockServer wireMock;
    private WebhookEventRepository eventRepository;
    private DestinationRepository destinationRepository;
    private DeliveryAttemptRepository attemptRepository;
    private RetryStateService retryStateService;
    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        eventRepository = mock(WebhookEventRepository.class);
        destinationRepository = mock(DestinationRepository.class);
        attemptRepository = mock(DeliveryAttemptRepository.class);
        retryStateService = mock(RetryStateService.class);

        deliveryService = new DeliveryService(eventRepository, destinationRepository, attemptRepository,
                retryStateService, new SimpleMeterRegistry(), RestClient.builder(), 3);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void marksEventDeliveredOn200() {
        wireMock.stubFor(post(urlEqualTo("/hook")).willReturn(aResponse().withStatus(200).withBody("ok")));
        prepare();

        deliveryService.deliver(new WebhookEventMessage(1L, "stripe:evt_1", "stripe", "payment.succeeded", 1));

        wireMock.verify(postRequestedFor(urlEqualTo("/hook")));
        verify(retryStateService, never()).scheduleRetry(any());
    }

    @Test
    void schedulesRetryOn500() {
        wireMock.stubFor(post(urlEqualTo("/hook")).willReturn(aResponse().withStatus(500)));
        prepare();

        deliveryService.deliver(new WebhookEventMessage(1L, "stripe:evt_1", "stripe", "payment.succeeded", 1));

        verify(retryStateService).scheduleRetry(any());
    }

    private void prepare() {
        WebhookEvent event = new WebhookEvent();
        event.setId(1L);
        event.setEventId("stripe:evt_1");
        event.setSource("stripe");
        event.setEventType("payment.succeeded");
        event.setPayload("{\"id\":\"evt_1\"}");
        event.setStatus(EventStatus.RECEIVED);

        Destination destination = new Destination();
        destination.setId(10L);
        destination.setName("Test destination");
        destination.setSource("stripe");
        destination.setActive(true);
        destination.setTargetUrl(wireMock.baseUrl() + "/hook");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(destinationRepository.findBySourceIgnoreCaseAndActiveTrue("stripe")).thenReturn(List.of(destination));
        when(attemptRepository.save(any(DeliveryAttempt.class))).thenAnswer(i -> i.getArgument(0));
    }
}
