package com.dbtech.scheduler.repository;

import com.dbtech.scheduler.entity.JobExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobExecutionStatusRepository extends JpaRepository<JobExecutionStatus, Long> {
}
