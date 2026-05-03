package com.zeotap.ims.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "signals")
public class Signal {

    @Id
    private String id;

    @Indexed
    private String componentId;

    private ComponentType componentType;

    private SignalType signalType;

    private Double value;

    private Double threshold;

    private String unit;

    private String region;

    private Instant timestamp;

    @Indexed
    private String workItemId;

    private Map<String, String> metadata;

    public enum ComponentType {
        RDBMS,
        API_GATEWAY,
        MCP_HOST,
        ASYNC_QUEUE,
        NOSQL,
        CACHE
    }

    public enum SignalType {
        LATENCY_SPIKE,
        CONNECTION_FAILURE,
        MEMORY_PRESSURE,
        CPU_SPIKE,
        ERROR_RATE_HIGH,
        HEALTH_CHECK_FAIL,
        CONNECTION_POOL_EXHAUSTED,
        QUEUE_DEPTH_HIGH
    }
}
