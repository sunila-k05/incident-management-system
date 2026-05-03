package com.zeotap.ims.controller;

import com.zeotap.ims.model.Rca;
import com.zeotap.ims.service.RcaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rca")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RcaController {

    private final RcaService rcaService;

    // Submit or update RCA for a work item
    @PostMapping("/{workItemId}")
    public ResponseEntity<?> submitRca(
            @PathVariable String workItemId,
            @RequestBody Rca rca) {
        try {
            Rca saved = rcaService.submitRca(workItemId, rca);
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Get RCA for a work item
    @GetMapping("/{workItemId}")
    public ResponseEntity<?> getRca(@PathVariable String workItemId) {
        try {
            return ResponseEntity.ok(
                rcaService.getRcaByWorkItemId(workItemId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
