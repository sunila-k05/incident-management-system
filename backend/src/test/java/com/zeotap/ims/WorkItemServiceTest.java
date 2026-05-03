package com.zeotap.ims;

import com.zeotap.ims.model.Rca;
import com.zeotap.ims.model.Signal;
import com.zeotap.ims.model.WorkItem;
import com.zeotap.ims.repository.RcaRepository;
import com.zeotap.ims.repository.WorkItemRepository;
import com.zeotap.ims.service.WorkItemService;
import com.zeotap.ims.statemachine.IncidentStateMachine;
import com.zeotap.ims.statemachine.ClosedState;
import com.zeotap.ims.statemachine.OpenState;
import com.zeotap.ims.statemachine.InvestigatingState;
import com.zeotap.ims.statemachine.ResolvedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkItemServiceTest {

    @Mock
    private WorkItemRepository workItemRepository;

    @Mock
    private RcaRepository rcaRepository;

    private WorkItemService workItemService;
    private WorkItem resolvedWorkItem;

    @BeforeEach
    void setUp() {
        IncidentStateMachine stateMachine = new IncidentStateMachine(
            new OpenState(),
            new InvestigatingState(),
            new ResolvedState(),
            new ClosedState()
        );

        workItemService = new WorkItemService(
            workItemRepository,
            rcaRepository,
            stateMachine
        );

        resolvedWorkItem = WorkItem.builder()
            .id("test-incident-001")
            .componentId("RDBMS_PRIMARY_01")
            .componentType(Signal.ComponentType.RDBMS)
            .priority(WorkItem.Priority.P0)
            .state(WorkItem.IncidentState.RESOLVED)
            .startTime(Instant.now().minusSeconds(3600))
            .signalCount(47)
            .createdAt(Instant.now().minusSeconds(3600))
            .updatedAt(Instant.now())
            .build();
    }

    @Test
    void closeShouldFailWhenRcaIsMissing() {
        when(workItemRepository.findById("test-incident-001"))
            .thenReturn(Optional.of(resolvedWorkItem));
        when(rcaRepository.findByWorkItemId("test-incident-001"))
            .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> workItemService.moveToClosed("test-incident-001")
        );

        assertTrue(ex.getMessage().contains("Cannot close incident without RCA"));
    }

    @Test
    void closeShouldFailWhenRcaIsIncomplete() {
        Rca incompleteRca = Rca.builder()
            .workItemId("test-incident-001")
            .rootCauseCategory(Rca.RootCauseCategory.INFRASTRUCTURE_FAILURE)
            .fixApplied("")
            .preventionSteps("Add more replicas")
            .incidentStart(Instant.now().minusSeconds(3600))
            .incidentEnd(Instant.now())
            .build();

        when(workItemRepository.findById("test-incident-001"))
            .thenReturn(Optional.of(resolvedWorkItem));
        when(rcaRepository.findByWorkItemId("test-incident-001"))
            .thenReturn(Optional.of(incompleteRca));

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> workItemService.moveToClosed("test-incident-001")
        );

        assertTrue(ex.getMessage().contains("Fix applied description is required"));
    }

    @Test
    void closeShouldSucceedWithCompleteRca() {
        Rca completeRca = Rca.builder()
            .workItemId("test-incident-001")
            .rootCauseCategory(Rca.RootCauseCategory.INFRASTRUCTURE_FAILURE)
            .fixApplied("Restarted primary DB and promoted replica")
            .preventionSteps("Add automated failover and increase replica count")
            .incidentStart(Instant.now().minusSeconds(3600))
            .incidentEnd(Instant.now())
            .submittedBy("engineer@zeotap.com")
            .submittedAt(Instant.now())
            .build();

        when(workItemRepository.findById("test-incident-001"))
            .thenReturn(Optional.of(resolvedWorkItem));
        when(rcaRepository.findByWorkItemId("test-incident-001"))
            .thenReturn(Optional.of(completeRca));
        when(workItemRepository.save(any(WorkItem.class)))
            .thenReturn(resolvedWorkItem);

        assertDoesNotThrow(
            () -> workItemService.moveToClosed("test-incident-001")
        );
    }

    @Test
    void shouldNotAllowDirectTransitionFromOpenToClosed() {
        WorkItem openWorkItem = WorkItem.builder()
            .id("test-incident-002")
            .state(WorkItem.IncidentState.OPEN)
            .build();

        when(workItemRepository.findById("test-incident-002"))
            .thenReturn(Optional.of(openWorkItem));
        when(rcaRepository.findByWorkItemId("test-incident-002"))
            .thenReturn(Optional.of(Rca.builder()
                .rootCauseCategory(Rca.RootCauseCategory.UNKNOWN)
                .fixApplied("fix")
                .preventionSteps("steps")
                .incidentStart(Instant.now().minusSeconds(100))
                .incidentEnd(Instant.now())
                .build()));

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> workItemService.moveToClosed("test-incident-002")
        );

        assertTrue(ex.getMessage().contains("RESOLVED"));
    }

    @Test
    void mttrShouldBeCalculatedOnClose() {
        Instant start = Instant.now().minusSeconds(7200);
        Instant end = Instant.now();

        resolvedWorkItem.setStartTime(start);

        Rca completeRca = Rca.builder()
            .workItemId("test-incident-001")
            .rootCauseCategory(Rca.RootCauseCategory.DEPLOYMENT_REGRESSION)
            .fixApplied("Rolled back deployment v2.3.1")
            .preventionSteps("Add canary deployment pipeline")
            .incidentStart(start)
            .incidentEnd(end)
            .build();

        when(workItemRepository.findById("test-incident-001"))
            .thenReturn(Optional.of(resolvedWorkItem));
        when(rcaRepository.findByWorkItemId("test-incident-001"))
            .thenReturn(Optional.of(completeRca));
        when(workItemRepository.save(any(WorkItem.class)))
            .thenAnswer(i -> i.getArguments()[0]);

        workItemService.moveToClosed("test-incident-001");

        assertNotNull(resolvedWorkItem.getMttrMinutes());
        assertTrue(resolvedWorkItem.getMttrMinutes() >= 119);
    }
}
