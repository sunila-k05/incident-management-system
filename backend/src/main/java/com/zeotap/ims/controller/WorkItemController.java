package com.zeotap.ims.controller;

import com.zeotap.ims.model.Signal;
import com.zeotap.ims.model.WorkItem;
import com.zeotap.ims.repository.SignalRepository;
import com.zeotap.ims.service.WorkItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkItemController {

    private final WorkItemService workItemService;
    private final SignalRepository signalRepository;

    // Live feed - all active incidents sorted by priority
    @GetMapping
    public ResponseEntity<List<WorkItem>> getActiveIncidents() {
        return ResponseEntity.ok(workItemService.getActiveIncidents());
    }

    // Incident detail
    @GetMapping("/{id}")
    public ResponseEntity<WorkItem> getById(@PathVariable String id) {
        return ResponseEntity.ok(workItemService.getById(id));
    }

    // Get raw signals for an incident - from MongoDB
    @GetMapping("/{id}/signals")
    public ResponseEntity<List<Signal>> getSignals(@PathVariable String id) {
        return ResponseEntity.ok(
            signalRepository.findByWorkItemId(id));
    }

    // Transition endpoints
    @PutMapping("/{id}/investigate")
    public ResponseEntity<WorkItem> investigate(@PathVariable String id) {
        return ResponseEntity.ok(
            workItemService.moveToInvestigating(id));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<WorkItem> resolve(@PathVariable String id) {
        return ResponseEntity.ok(
            workItemService.moveToResolved(id));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable String id) {
        try {
            return ResponseEntity.ok(workItemService.moveToClosed(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
