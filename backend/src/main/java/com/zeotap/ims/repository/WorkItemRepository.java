package com.zeotap.ims.repository;

import com.zeotap.ims.model.WorkItem;
import com.zeotap.ims.model.Signal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, String> {

    // Get all active incidents sorted by priority - used in live feed
    List<WorkItem> findByStateNotOrderByPriorityAscCreatedAtDesc(
        WorkItem.IncidentState state
    );

    // Get incidents by state
    List<WorkItem> findByStateOrderByPriorityAscCreatedAtDesc(
        WorkItem.IncidentState state
    );

    // Get incidents by component
    List<WorkItem> findByComponentIdOrderByCreatedAtDesc(String componentId);

    // Check if active window exists for component - used by debounce
    @Query("SELECT w FROM WorkItem w WHERE w.componentId = ?1 " +
           "AND w.state NOT IN ('CLOSED') " +
           "ORDER BY w.createdAt DESC")
    List<WorkItem> findActiveByComponentId(String componentId);
}
