package com.dbtech.scheduler.services.scheduler;

import com.dbtech.scheduler.dto.request.JobRequest;
import java.util.List;

import com.dbtech.scheduler.dto.response.JobDetailDto;


public interface
SchedulerService {

    String scheduleJob(JobRequest job);

    List<JobDetailDto> listJobs();

    String deletejob(String jobName, String jobGroup);

    String pauseJob(String jobName, String jobGroup);

    String restartJob(String jobName, String jobGroup);

    String updateJob(String jobName, JobRequest jobRequest);
}
