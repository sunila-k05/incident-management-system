package com.zeotap.ims.statemachine;

import com.zeotap.ims.model.WorkItem;

public interface IncidentState {

    // Move to next state
    void investigate(WorkItem workItem);
    void resolve(WorkItem workItem);
    void close(WorkItem workItem);

    // Get current state name
    WorkItem.IncidentState getStateName();
}
