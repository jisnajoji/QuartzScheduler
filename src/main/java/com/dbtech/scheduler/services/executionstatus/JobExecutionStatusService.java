package com.dbtech.scheduler.services.executionstatus;


public interface JobExecutionStatusService {
    void logJobExecution(String jobName, String jobGroup, boolean success, String failureReason);
}
