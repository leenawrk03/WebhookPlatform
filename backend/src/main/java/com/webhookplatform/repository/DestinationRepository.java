package com.webhookplatform.repository;

import com.webhookplatform.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findBySourceIgnoreCaseAndActiveTrue(String source);
    List<Destination> findAllByOrderByCreatedAtDesc();
}
