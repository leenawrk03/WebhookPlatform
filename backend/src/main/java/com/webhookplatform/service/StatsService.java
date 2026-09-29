package com.webhookplatform.service;

import com.webhookplatform.dto.Dtos.StatsResponse;
import com.webhookplatform.entity.DeliveryStatus;
import com.webhookplatform.repository.DeliveryAttemptRepository;
import com.webhookplatform.repository.DestinationRepository;
import com.webhookplatform.repository.WebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {

    private final WebhookEventRepository eventRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final DestinationRepository destinationRepository;

    public StatsService(WebhookEventRepository eventRepository,
                        DeliveryAttemptRepository attemptRepository,
                        DestinationRepository destinationRepository) {
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
        this.destinationRepository = destinationRepository;
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        long received = eventRepository.count();
        long success = attemptRepository.countByStatus(DeliveryStatus.SUCCESS);
        long failed = attemptRepository.countByStatus(DeliveryStatus.FAILED);
        long retries = attemptRepository.countByAttemptNumberGreaterThan(1);
        long total = success + failed;
        double rate = total == 0 ? 0d : Math.round((success * 10000d) / total) / 100d;
        long activeDestinations = destinationRepository.findAllByOrderByCreatedAtDesc()
                .stream().filter(d -> d.isActive()).count();
        return new StatsResponse(received, success, failed, retries, rate, activeDestinations);
    }
}
