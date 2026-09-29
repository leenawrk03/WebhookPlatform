package com.webhookplatform.repository;

import com.webhookplatform.entity.EventStatus;
import com.webhookplatform.entity.WebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    Optional<WebhookEvent> findByEventId(String eventId);
    Page<WebhookEvent> findBySourceIgnoreCase(String source, Pageable pageable);
    Page<WebhookEvent> findByStatus(EventStatus status, Pageable pageable);
    long countByStatus(EventStatus status);
}
