package com.dbtech.scheduler.dto.request;

import lombok.*;

import java.util.Date;

/**
 * Request body for Job.
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JobRequest {
    private String jobName;
    private String jobGroup;
    private ScheduleType scheduleType;
    private String cronExpression;
    private long fixedDelay;
    private long fixedRate;
    private Date startTime;
}
