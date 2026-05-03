package com.zeotap.ims.strategy;

import com.zeotap.ims.model.Signal;
import com.zeotap.ims.model.WorkItem;

public interface AlertStrategy {

    // Determines priority based on component type
    WorkItem.Priority getPriority();

    // Generates a meaningful incident title
    String generateTitle(Signal signal);

    // Describes what this alert means in plain English
    String getDescription(Signal signal);
}
