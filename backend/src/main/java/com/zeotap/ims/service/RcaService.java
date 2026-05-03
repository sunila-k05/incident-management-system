package com.zeotap.ims.service;

import com.zeotap.ims.model.Rca;
import com.zeotap.ims.model.WorkItem;
import com.zeotap.ims.repository.RcaRepository;
import com.zeotap.ims.repository.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RcaService {

    private final RcaRepository rcaRepository;
    private final WorkItemRepository workItemRepository;

    @Transactional
    public Rca submitRca(String workItemId, Rca rca) {
        // Verify work item exists
        WorkItem workItem = workItemRepository.findById(workItemId)
            .orElseThrow(() -> new RuntimeException(
                "Work item not found: " + workItemId));

        // Cannot submit RCA for already closed incident
        if (workItem.getState() == WorkItem.IncidentState.CLOSED) {
            throw new IllegalStateException(
                "Cannot submit RCA for a CLOSED incident.");
        }

        // Check if RCA already exists - update it
        Rca existing = rcaRepository.findByWorkItemId(workItemId)
            .orElse(null);

        if (existing != null) {
            existing.setRootCauseCategory(rca.getRootCauseCategory());
            existing.setFixApplied(rca.getFixApplied());
            existing.setPreventionSteps(rca.getPreventionSteps());
            existing.setIncidentStart(rca.getIncidentStart());
            existing.setIncidentEnd(rca.getIncidentEnd());
            existing.setSubmittedBy(rca.getSubmittedBy());
            existing.setSubmittedAt(Instant.now());
            log.info("RCA updated for work item: {}", workItemId);
            return rcaRepository.save(existing);
        }

        // New RCA
        rca.setWorkItemId(workItemId);
        rca.setSubmittedAt(Instant.now());
        log.info("RCA submitted for work item: {}", workItemId);
        return rcaRepository.save(rca);
    }

    public Rca getRcaByWorkItemId(String workItemId) {
        return rcaRepository.findByWorkItemId(workItemId)
            .orElseThrow(() -> new RuntimeException(
                "No RCA found for work item: " + workItemId));
    }
}
