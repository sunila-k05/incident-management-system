package com.zeotap.ims.statemachine;

import com.zeotap.ims.model.WorkItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncidentStateMachine {

    private final OpenState openState;
    private final InvestigatingState investigatingState;
    private final ResolvedState resolvedState;
    private final ClosedState closedState;

    private IncidentState getState(WorkItem.IncidentState state) {
        return switch (state) {
            case OPEN -> openState;
            case INVESTIGATING -> investigatingState;
            case RESOLVED -> resolvedState;
            case CLOSED -> closedState;
        };
    }

    public void transitionToInvestigating(WorkItem workItem) {
        getState(workItem.getState()).investigate(workItem);
    }

    public void transitionToResolved(WorkItem workItem) {
        getState(workItem.getState()).resolve(workItem);
    }

    public void transitionToClosed(WorkItem workItem) {
        getState(workItem.getState()).close(workItem);
    }
}
