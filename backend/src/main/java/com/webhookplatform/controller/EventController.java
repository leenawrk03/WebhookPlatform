package com.webhookplatform.controller;

import com.webhookplatform.dto.Dtos.*;
import com.webhookplatform.entity.Destination;
import com.webhookplatform.entity.WebhookEvent;
import com.webhookplatform.repository.DeliveryAttemptRepository;
import com.webhookplatform.repository.DestinationRepository;
import com.webhookplatform.repository.WebhookEventRepository;
import com.webhookplatform.service.StatsService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EventController {

    private final WebhookEventRepository eventRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final DestinationRepository destinationRepository;
    private final StatsService statsService;

    public EventController(WebhookEventRepository eventRepository,
                           DeliveryAttemptRepository attemptRepository,
                           DestinationRepository destinationRepository,
                           StatsService statsService) {
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
        this.destinationRepository = destinationRepository;
        this.statsService = statsService;
    }

    @GetMapping("/events")
    public Page<EventResponse> events(@RequestParam(required = false) String source,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<WebhookEvent> events = (source == null || source.isBlank())
                ? eventRepository.findAll(pageable)
                : eventRepository.findBySourceIgnoreCase(source, pageable);
        return events.map(this::toEventResponse);
    }

    @GetMapping("/events/{id}")
    public EventDetailResponse event(@PathVariable Long id) {
        WebhookEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        Map<Long, String> names = destinationNames();
        List<DeliveryAttemptResponse> attempts = attemptRepository.findByEventIdOrderByAttemptedAtAsc(id)
                .stream()
                .map(a -> new DeliveryAttemptResponse(a.getId(), a.getEventId(), a.getDestinationId(),
                        names.getOrDefault(a.getDestinationId(), "deleted destination"),
                        a.getAttemptNumber(), a.getStatus().name(), a.getResponseStatus(),
                        a.getErrorMessage(), a.getDurationMs(), a.getAttemptedAt()))
                .toList();
        return new EventDetailResponse(event.getId(), event.getEventId(), event.getSource(),
                event.getEventType(), event.getStatus().name(), event.getReceivedAt(),
                event.getPayload(), event.getHeaders(), attempts);
    }

    @GetMapping("/deliveries")
    public Page<DeliveryAttemptResponse> deliveries(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Map<Long, String> names = destinationNames();
        return attemptRepository
                .findAllByOrderByAttemptedAtDesc(PageRequest.of(page, Math.min(size, 100)))
                .map(a -> new DeliveryAttemptResponse(a.getId(), a.getEventId(), a.getDestinationId(),
                        names.getOrDefault(a.getDestinationId(), "deleted destination"),
                        a.getAttemptNumber(), a.getStatus().name(), a.getResponseStatus(),
                        a.getErrorMessage(), a.getDurationMs(), a.getAttemptedAt()));
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return statsService.stats();
    }

    private Map<Long, String> destinationNames() {
        return destinationRepository.findAll().stream()
                .collect(Collectors.toMap(Destination::getId, Destination::getName, (a, b) -> a));
    }

    private EventResponse toEventResponse(WebhookEvent e) {
        return new EventResponse(e.getId(), e.getEventId(), e.getSource(),
                e.getEventType(), e.getStatus().name(), e.getReceivedAt());
    }
}
