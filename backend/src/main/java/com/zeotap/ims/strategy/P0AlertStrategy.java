package com.zeotap.ims.strategy;

import com.zeotap.ims.model.Signal;
import com.zeotap.ims.model.WorkItem;
import org.springframework.stereotype.Component;

@Component
public class P0AlertStrategy implements AlertStrategy {

    @Override
    public WorkItem.Priority getPriority() {
        return WorkItem.Priority.P0;
    }

    @Override
    public String generateTitle(Signal signal) {
        return String.format("[P0] %s - %s detected on %s",
                signal.getComponentType(),
                signal.getSignalType(),
                signal.getComponentId());
    }

    @Override
    public String getDescription(Signal signal) {
        return String.format(
            "CRITICAL: %s is experiencing %s. " +
            "Measured value: %.2f %s (threshold: %.2f %s). " +
            "Region: %s. Immediate action required.",
            signal.getComponentId(),
            signal.getSignalType(),
            signal.getValue(), signal.getUnit(),
            signal.getThreshold(), signal.getUnit(),
            signal.getRegion()
        );
    }
}
