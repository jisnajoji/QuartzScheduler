package com.dbtech.scheduler.dto.request;

import lombok.*;

/**
 * Request body for Job.
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JobRequest {
    private String jobId;
    private String jobName;
    private String cron;
    private Long delayInMillis;
    private Boolean isStopped;
}
