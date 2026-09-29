package com.webhookplatform.service;

import com.webhookplatform.dto.Dtos.DestinationRequest;
import com.webhookplatform.dto.Dtos.DestinationResponse;
import com.webhookplatform.entity.Destination;
import com.webhookplatform.repository.DestinationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
public class DestinationService {

    private final DestinationRepository repository;

    public DestinationService(DestinationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DestinationResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public DestinationResponse create(DestinationRequest request) {
        Destination destination = new Destination();
        apply(destination, request);
        return toResponse(repository.save(destination));
    }

    @Transactional
    public DestinationResponse update(Long id, DestinationRequest request) {
        Destination destination = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found"));
        apply(destination, request);
        return toResponse(repository.save(destination));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination not found");
        }
        repository.deleteById(id);
    }

    private void apply(Destination destination, DestinationRequest request) {
        destination.setName(request.name().trim());
        destination.setTargetUrl(request.targetUrl().trim());
        destination.setSource(request.source().trim().toLowerCase());
        destination.setSecret(request.secret());
        destination.setActive(request.active() == null || request.active());
    }

    private DestinationResponse toResponse(Destination d) {
        return new DestinationResponse(d.getId(), d.getName(), d.getTargetUrl(),
                d.getSource(), d.isActive(), d.getCreatedAt());
    }
}
