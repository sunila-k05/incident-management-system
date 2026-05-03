package com.zeotap.ims.statemachine;

import com.zeotap.ims.model.WorkItem;
import org.springframework.stereotype.Component;

@Component
public class InvestigatingState implements IncidentState {

    @Override
    public void investigate(WorkItem workItem) {
        throw new IllegalStateException(
            "Incident is already in INVESTIGATING state."
        );
    }

    @Override
    public void resolve(WorkItem workItem) {
        workItem.setState(WorkItem.IncidentState.RESOLVED);
    }

    @Override
    public void close(WorkItem workItem) {
        throw new IllegalStateException(
            "Cannot close incident from INVESTIGATING state. " +
            "Must be RESOLVED with a complete RCA first."
        );
    }

    @Override
    public WorkItem.IncidentState getStateName() {
        return WorkItem.IncidentState.INVESTIGATING;
    }
}
