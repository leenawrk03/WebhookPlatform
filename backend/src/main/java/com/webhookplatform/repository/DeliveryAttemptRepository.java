package com.webhookplatform.repository;

import com.webhookplatform.entity.DeliveryAttempt;
import com.webhookplatform.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {
    List<DeliveryAttempt> findByEventIdOrderByAttemptedAtAsc(Long eventId);
    Page<DeliveryAttempt> findAllByOrderByAttemptedAtDesc(Pageable pageable);
    long countByStatus(DeliveryStatus status);
    long countByStatusAndAttemptNumberGreaterThan(DeliveryStatus status, int attemptNumber);
    long countByAttemptNumberGreaterThan(int attemptNumber);
}
