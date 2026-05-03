package com.zeotap.ims.service;

import com.zeotap.ims.model.Rca;
import com.zeotap.ims.model.WorkItem;
import com.zeotap.ims.repository.RcaRepository;
import com.zeotap.ims.repository.WorkItemRepository;
import com.zeotap.ims.statemachine.IncidentStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final RcaRepository rcaRepository;
    private final IncidentStateMachine stateMachine;

    // Get all active incidents for live feed
    public List<WorkItem> getActiveIncidents() {
        return workItemRepository
            .findByStateNotOrderByPriorityAscCreatedAtDesc(
                WorkItem.IncidentState.CLOSED
            );
    }

    public WorkItem getById(String id) {
        return workItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Work item not found: " + id));
    }

    // Transition to INVESTIGATING
    @Transactional
    public WorkItem moveToInvestigating(String id) {
        WorkItem workItem = getById(id);
        stateMachine.transitionToInvestigating(workItem);
        workItem.setUpdatedAt(Instant.now());
        WorkItem saved = workItemRepository.save(workItem);
        log.info("Incident {} moved to INVESTIGATING", id);
        return saved;
    }

    // Transition to RESOLVED
    @Transactional
    public WorkItem moveToResolved(String id) {
        WorkItem workItem = getById(id);
        stateMachine.transitionToResolved(workItem);
        workItem.setUpdatedAt(Instant.now());
        WorkItem saved = workItemRepository.save(workItem);
        log.info("Incident {} moved to RESOLVED", id);
        return saved;
    }

    // Transition to CLOSED - mandatory RCA validation
    @Transactional
    public WorkItem moveToClosed(String id) {
        WorkItem workItem = getById(id);

        // Mandatory RCA check - reject if missing or incomplete
        Rca rca = rcaRepository.findByWorkItemId(id)
            .orElseThrow(() -> new IllegalStateException(
                "Cannot close incident without RCA. " +
                "Please submit a complete RCA first."
            ));

        validateRca(rca);

        // Calculate MTTR
        if (workItem.getStartTime() != null && rca.getIncidentEnd() != null) {
            long mttr = ChronoUnit.MINUTES.between(
                workItem.getStartTime(),
                rca.getIncidentEnd()
            );
            workItem.setMttrMinutes(mttr);
            workItem.setEndTime(rca.getIncidentEnd());
            log.info("MTTR for incident {}: {} minutes", id, mttr);
        }

        stateMachine.transitionToClosed(workItem);
        workItem.setUpdatedAt(Instant.now());
        WorkItem saved = workItemRepository.save(workItem);
        log.info("Incident {} CLOSED. MTTR: {} minutes",
            id, workItem.getMttrMinutes());
        return saved;
    }

    // RCA validation - all fields must be present
    private void validateRca(Rca rca) {
        if (rca.getRootCauseCategory() == null) {
            throw new IllegalStateException(
                "RCA incomplete: Root cause category is required.");
        }
        if (rca.getFixApplied() == null || rca.getFixApplied().isBlank()) {
            throw new IllegalStateException(
                "RCA incomplete: Fix applied description is required.");
        }
        if (rca.getPreventionSteps() == null || rca.getPreventionSteps().isBlank()) {
            throw new IllegalStateException(
                "RCA incomplete: Prevention steps are required.");
        }
        if (rca.getIncidentStart() == null || rca.getIncidentEnd() == null) {
            throw new IllegalStateException(
                "RCA incomplete: Incident start and end times are required.");
        }
    }
}
