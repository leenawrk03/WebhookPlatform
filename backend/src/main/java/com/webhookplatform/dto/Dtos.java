package com.webhookplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public final class Dtos {

    private Dtos() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record LoginResponse(String token, String username, long expiresInSeconds) {}

    public record DestinationRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "^https?://.+", message = "targetUrl must be a http(s) URL")
            @Size(max = 2048) String targetUrl,
            @NotBlank @Size(max = 60) String source,
            @Size(max = 255) String secret,
            Boolean active
    ) {}

    public record DestinationResponse(
            Long id, String name, String targetUrl, String source,
            boolean active, OffsetDateTime createdAt
    ) {}

    public record EventResponse(
            Long id, String eventId, String source, String eventType,
            String status, OffsetDateTime receivedAt
    ) {}

    public record EventDetailResponse(
            Long id, String eventId, String source, String eventType, String status,
            OffsetDateTime receivedAt, String payload, String headers,
            java.util.List<DeliveryAttemptResponse> attempts
    ) {}

    public record DeliveryAttemptResponse(
            Long id, Long eventId, Long destinationId, String destinationName,
            int attemptNumber, String status, Integer responseStatus,
            String errorMessage, Long durationMs, OffsetDateTime attemptedAt
    ) {}

    public record IngestResponse(String eventId, String status) {}

    public record StatsResponse(
            long eventsReceived,
            long successfulDeliveries,
            long failedDeliveries,
            long retries,
            double successRate,
            long activeDestinations
    ) {}
}
