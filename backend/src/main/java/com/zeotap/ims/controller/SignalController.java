package com.zeotap.ims.controller;

import com.zeotap.ims.model.Signal;
import com.zeotap.ims.service.SignalIngestionService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SignalController {

    private final SignalIngestionService ingestionService;

    // Rate limiter - 10,000 requests per second
    private final Bucket rateLimiter = Bucket.builder()
        .addLimit(Bandwidth.classic(10_000,
            Refill.greedy(10_000, Duration.ofSeconds(1))))
        .build();

    // Single signal ingestion
    @PostMapping
    public ResponseEntity<Map<String, String>> ingest(
            @RequestBody Signal signal) {

        if (!rateLimiter.tryConsume(1)) {
            log.warn("Rate limit exceeded for component: {}",
                signal.getComponentId());
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "status", "rejected",
                    "reason", "Rate limit exceeded. Try again shortly."
                ));
        }

        ingestionService.ingest(signal);

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(Map.of(
                "status", "accepted",
                "message", "Signal queued for processing"
            ));
    }

    // Batch signal ingestion - for high throughput scenarios
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> ingestBatch(
            @RequestBody List<Signal> signals) {

        if (!rateLimiter.tryConsume(signals.size())) {
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                    "status", "rejected",
                    "reason", "Rate limit exceeded.",
                    "signalCount", signals.size()
                ));
        }

        signals.forEach(ingestionService::ingest);

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(Map.of(
                "status", "accepted",
                "message", "Signals queued for processing",
                "signalCount", signals.size()
            ));
    }
}
