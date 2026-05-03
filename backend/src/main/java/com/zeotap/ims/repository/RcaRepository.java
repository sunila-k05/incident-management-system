package com.zeotap.ims.repository;

import com.zeotap.ims.model.Rca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RcaRepository extends JpaRepository<Rca, String> {

    // Get RCA for a specific work item
    Optional<Rca> findByWorkItemId(String workItemId);

    // Check if RCA exists for work item
    boolean existsByWorkItemId(String workItemId);
}
