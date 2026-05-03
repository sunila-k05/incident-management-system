package com.zeotap.ims.statemachine;

import com.zeotap.ims.model.WorkItem;
import org.springframework.stereotype.Component;

@Component
public class ClosedState implements IncidentState {

    @Override
    public void investigate(WorkItem workItem) {
        throw new IllegalStateException(
            "Incident is CLOSED. No further transitions allowed."
        );
    }

    @Override
    public void resolve(WorkItem workItem) {
        throw new IllegalStateException(
            "Incident is CLOSED. No further transitions allowed."
        );
    }

    @Override
    public void close(WorkItem workItem) {
        throw new IllegalStateException(
            "Incident is already CLOSED."
        );
    }

    @Override
    public WorkItem.IncidentState getStateName() {
        return WorkItem.IncidentState.CLOSED;
    }
}
