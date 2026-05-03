package com.zeotap.ims.statemachine;

import com.zeotap.ims.model.WorkItem;
import com.zeotap.ims.model.Rca;
import org.springframework.stereotype.Component;

@Component
public class ResolvedState implements IncidentState {

    @Override
    public void investigate(WorkItem workItem) {
        throw new IllegalStateException(
            "Cannot move back to INVESTIGATING from RESOLVED state."
        );
    }

    @Override
    public void resolve(WorkItem workItem) {
        throw new IllegalStateException(
            "Incident is already in RESOLVED state."
        );
    }

    @Override
    public void close(WorkItem workItem) {
        // RCA validation happens in the service layer before this is called
        workItem.setState(WorkItem.IncidentState.CLOSED);
    }

    @Override
    public WorkItem.IncidentState getStateName() {
        return WorkItem.IncidentState.RESOLVED;
    }
}
