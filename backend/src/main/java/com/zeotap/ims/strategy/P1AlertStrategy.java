package com.zeotap.ims.strategy;

import com.zeotap.ims.model.Signal;
import com.zeotap.ims.model.WorkItem;
import org.springframework.stereotype.Component;

@Component
public class P1AlertStrategy implements AlertStrategy {

    @Override
    public WorkItem.Priority getPriority() {
        return WorkItem.Priority.P1;
    }

    @Override
    public String generateTitle(Signal signal) {
        return String.format("[P1] %s - %s detected on %s",
                signal.getComponentType(),
                signal.getSignalType(),
                signal.getComponentId());
    }

    @Override
    public String getDescription(Signal signal) {
        return String.format(
            "HIGH: %s is experiencing %s. " +
            "Measured value: %.2f %s (threshold: %.2f %s). " +
            "Region: %s. Investigate promptly.",
            signal.getComponentId(),
            signal.getSignalType(),
            signal.getValue(), signal.getUnit(),
            signal.getThreshold(), signal.getUnit(),
            signal.getRegion()
        );
    }
}
