package com.nowrongdoor.repository;

import com.nowrongdoor.model.IngestionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IngestionRunRepository extends JpaRepository<IngestionRun, Long> {

    /** Returns the most recently started ingestion run. */
    Optional<IngestionRun> findTopByOrderByStartedAtDesc();
}
