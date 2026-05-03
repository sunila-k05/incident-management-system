package com.zeotap.ims.repository;

import com.zeotap.ims.model.Signal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignalRepository extends MongoRepository<Signal, String> {

    // Get all signals for a specific work item - used in incident detail UI
    List<Signal> findByWorkItemId(String workItemId);

    // Get all signals for a component - used for history view
    List<Signal> findByComponentIdOrderByTimestampDesc(String componentId);

    // Count signals per work item
    long countByWorkItemId(String workItemId);
}
