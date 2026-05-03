package com.zeotap.ims.statemachine;

import com.zeotap.ims.model.WorkItem;
import org.springframework.stereotype.Component;

@Component
public class OpenState implements IncidentState {

    @Override
    public void investigate(WorkItem workItem) {
        workItem.setState(WorkItem.IncidentState.INVESTIGATING);
    }

    @Override
    public void resolve(WorkItem workItem) {
        throw new IllegalStateException(
            "Cannot resolve incident from OPEN state. " +
            "Must move to INVESTIGATING first."
        );
    }

    @Override
    public void close(WorkItem workItem) {
        throw new IllegalStateException(
            "Cannot close incident from OPEN state. " +
            "Must go through INVESTIGATING and RESOLVED first."
        );
    }

    @Override
    public WorkItem.IncidentState getStateName() {
        return WorkItem.IncidentState.OPEN;
    }
}
