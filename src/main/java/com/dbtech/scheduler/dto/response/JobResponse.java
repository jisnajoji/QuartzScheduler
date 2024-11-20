package com.dbtech.scheduler.dto.response;

import lombok.*;

/**
 * Job response.
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JobResponse {
    private Long id;
    private String jobId;
    private String jobName;
    private String cron;
    private Long delayInMillis;
    private Boolean isStopped;
}
