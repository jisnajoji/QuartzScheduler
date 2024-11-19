package com.dbtech.scheduler.repository;

import com.dbtech.scheduler.entity.Job;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Job repository.
 */
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByIsStoppedFalse();
}
