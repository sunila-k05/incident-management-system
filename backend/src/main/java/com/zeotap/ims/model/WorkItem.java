package com.zeotap.ims.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "work_items")
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Which component triggered this incident
    private String componentId;

    @Enumerated(EnumType.STRING)
    private Signal.ComponentType componentType;

    // Priority determined by Strategy pattern
    @Enumerated(EnumType.STRING)
    private Priority priority;

    // Current state - managed by State pattern
    @Enumerated(EnumType.STRING)
    private IncidentState state;

    // When first signal arrived
    private Instant startTime;

    // When RCA was submitted (used for MTTR)
    private Instant endTime;

    // MTTR in minutes - auto calculated
    private Long mttrMinutes;

    // How many raw signals are linked to this work item
    private Integer signalCount;

    // Short description auto-generated from component + signal type
    private String title;

    private Instant createdAt;
    private Instant updatedAt;

    public enum Priority {
        P0, P1, P2
    }

    public enum IncidentState {
        OPEN,
        INVESTIGATING,
        RESOLVED,
        CLOSED
    }
}
