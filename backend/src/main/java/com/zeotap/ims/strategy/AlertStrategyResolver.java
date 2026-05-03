package com.zeotap.ims.strategy;

import com.zeotap.ims.model.Signal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertStrategyResolver {

    private final P0AlertStrategy p0Strategy;
    private final P1AlertStrategy p1Strategy;
    private final P2AlertStrategy p2Strategy;

    public AlertStrategy resolve(Signal.ComponentType componentType) {
        return switch (componentType) {
            // P0 - complete outage risk
            case RDBMS, API_GATEWAY -> p0Strategy;
            // P1 - significant degradation
            case MCP_HOST, ASYNC_QUEUE, NOSQL -> p1Strategy;
            // P2 - performance degradation only
            case CACHE -> p2Strategy;
        };
    }
}
