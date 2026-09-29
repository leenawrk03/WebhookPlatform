package com.webhookplatform.controller;

import com.webhookplatform.dto.Dtos.DestinationRequest;
import com.webhookplatform.dto.Dtos.DestinationResponse;
import com.webhookplatform.service.DestinationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationService service;

    public DestinationController(DestinationService service) {
        this.service = service;
    }

    @GetMapping
    public List<DestinationResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DestinationResponse create(@Valid @RequestBody DestinationRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public DestinationResponse update(@PathVariable Long id, @Valid @RequestBody DestinationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
