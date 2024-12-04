package com.dbtech.scheduler.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.time.LocalDateTime;

public class JobProcess implements Job {
    @Override
    public void execute(JobExecutionContext context) {
        System.out.println("Executing job: " + context.getJobDetail().getKey() +"\t executed at \t" + LocalDateTime.now());
    }
}
