package com.webhookplatform.controller;

import com.webhookplatform.dto.Dtos.IngestResponse;
import com.webhookplatform.entity.WebhookEvent;
import com.webhookplatform.service.WebhookIngestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Public endpoint external platforms post their webhooks to. */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookIngestController {

    private final WebhookIngestService ingestService;

    public WebhookIngestController(WebhookIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/{source}")
    public ResponseEntity<IngestResponse> receive(@PathVariable String source,
                                                  @RequestBody String body,
                                                  HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames())
                .forEach(name -> headers.put(name.toLowerCase(), request.getHeader(name)));

        WebhookEvent event = ingestService.ingest(source, body, headers);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new IngestResponse(event.getEventId(), event.getStatus().name()));
    }
}
