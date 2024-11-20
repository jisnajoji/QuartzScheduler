package com.dbtech.scheduler.services;

import com.dbtech.scheduler.dto.request.JobRequest;
import com.dbtech.scheduler.entity.Job;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface SchedulerService {
    List<Job> getScheduledJobs();

    void deleteScheduledJob(Long id);

    Job getJobById(Long id);

    void restartJob(Long id);

    void stopJob(Long id);

    void scheduleJob(JobRequest job);

    void updateScheduledJob(Long id, JobRequest job) throws BadRequestException;
}
