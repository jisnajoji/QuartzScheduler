package com.dbtech.scheduler.configuration;

import com.dbtech.scheduler.services.executionstatus.JobExecutionStatusService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JobExecutionStatusListener extends JobListenerSupport {

    @Autowired
    private JobExecutionStatusService jobExecutionStatusService;

    @Override
    public String getName() {
        return "JobExecutionStatusListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        context.getJobDetail().getKey().getName();
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        boolean success = (jobException == null);
        String failureReason = success ? null : jobException.getMessage();
        jobExecutionStatusService.logJobExecution(jobName, jobGroup, success, failureReason);
    }
}
