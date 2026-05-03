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
@Table(name = "rca_records")
public class Rca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Links back to the Work Item
    private String workItemId;

    // Incident timeline
    private Instant incidentStart;
    private Instant incidentEnd;

    // Structured root cause - dropdown in UI
    @Enumerated(EnumType.STRING)
    private RootCauseCategory rootCauseCategory;

    // What was done to fix it
    @Column(columnDefinition = "TEXT")
    private String fixApplied;

    // How to prevent recurrence
    @Column(columnDefinition = "TEXT")
    private String preventionSteps;

    // Who submitted the RCA
    private String submittedBy;

    private Instant submittedAt;

    public enum RootCauseCategory {
        INFRASTRUCTURE_FAILURE,
        DEPLOYMENT_REGRESSION,
        TRAFFIC_SPIKE,
        DEPENDENCY_FAILURE,
        CONFIGURATION_ERROR,
        HARDWARE_FAILURE,
        NETWORK_ISSUE,
        UNKNOWN
    }
}
