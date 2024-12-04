package com.dbtech.scheduler.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Job execution status.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "job_execution_status")
@EntityListeners(AuditingEntityListener.class)
public class JobExecutionStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String jobName;
    private String jobGroup;
    private LocalDateTime executionTime;
    private boolean success;
    private String failureReason;


    @CreatedDate
    private LocalDateTime createdAt;

    @CreatedBy
    private String createdBy;
}
