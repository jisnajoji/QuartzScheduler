package com.dbtech.scheduler.services.scheduler;

import com.dbtech.scheduler.configuration.JobExecutionStatusListener;
import com.dbtech.scheduler.dto.request.JobRequest;
import com.dbtech.scheduler.dto.response.JobDetailDto;
import com.dbtech.scheduler.job.JobProcess;

import java.util.*;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Scheduler service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerServiceImpl implements SchedulerService {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JobExecutionStatusListener jobExecutionStatusListener;

    @PostConstruct
    public void init() {
        try {
            scheduler.getListenerManager().addJobListener(jobExecutionStatusListener);
        } catch (SchedulerException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to register job listener", e);
        }
    }

    @Override
    public String scheduleJob(JobRequest jobRequest) {
        try {
            JobDetail jobDetail = createJobDetail(jobRequest);
            Trigger trigger = createTrigger(jobRequest);

            if (scheduler.checkExists(jobDetail.getKey())) {
                return "Job with the same name already exists!";
            }
            scheduler.scheduleJob(jobDetail, trigger);
            return "Job scheduled successfully and persisted!";
        } catch (SchedulerException e) {
            e.printStackTrace();
            return "Error scheduling job: " + e.getMessage();
        }
    }

    @Override
    public List<JobDetailDto> listJobs() {
        List<JobDetailDto> jobDetailsList = new ArrayList<>();
        try {
            for (String groupName : scheduler.getJobGroupNames()) {
                Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
                for (JobKey jobKey : jobKeys) {
                    JobDetail jobDetail = scheduler.getJobDetail(jobKey);

                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                    for (Trigger trigger : triggers) {
                        String triggerType = trigger instanceof CronTrigger ? "CronTrigger" : "SimpleTrigger";
                        String triggerExpression = trigger instanceof CronTrigger
                                ? ((CronTrigger) trigger).getCronExpression()
                                : String.valueOf(((SimpleTrigger) trigger).getRepeatInterval());

                        jobDetailsList.add(new JobDetailDto(
                                jobDetail.getKey().getName(),
                                jobDetail.getKey().getGroup(),
                                triggerType,
                                triggerExpression
                        ));
                    }
                }
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return jobDetailsList;
    }

    @Override
    public String deletejob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = new JobKey(jobName, jobGroup);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                return "Job deleted successfully!";
            } else {
                return "Job not found!";
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
            return "Error deleting job: " + e.getMessage();
        }
    }

    @Override
    public String pauseJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = new JobKey(jobName, jobGroup);
            if (scheduler.checkExists(jobKey)) {
                scheduler.pauseJob(jobKey);
                return "Job paused successfully!";
            } else {
                return "Job not found!";
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
            return "Error pausing job: " + e.getMessage();
        }
    }

    @Override
    public String restartJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = new JobKey(jobName, jobGroup);
            if (scheduler.checkExists(jobKey)) {
                scheduler.resumeJob(jobKey);
                return "Job restarted successfully!";
            } else {
                return "Job not found!";
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
            return "Error restarting job: " + e.getMessage();
        }
    }

    @Override
    public String updateJob(String jobName, JobRequest jobRequest) {
        try {
            JobKey jobKey = new JobKey(jobName, jobRequest.getJobGroup());

            if (!scheduler.checkExists(jobKey)) {
                return "Job with name '" + jobName + "' does not exist!";
            }

            boolean isPaused = scheduler.getTriggerState(new TriggerKey(jobName + "_trigger", jobRequest.getJobGroup()))
                    == Trigger.TriggerState.PAUSED;

            scheduler.pauseJob(jobKey);
            scheduler.deleteJob(jobKey);
            JobDetail newJobDetail = createJobDetail(jobRequest);
            Trigger newTrigger = createTrigger(jobRequest);
            scheduler.scheduleJob(newJobDetail, newTrigger);

            if (isPaused) {
                scheduler.pauseJob(newJobDetail.getKey());
            }

            return "Job updated successfully";
        } catch (SchedulerException e) {
            e.printStackTrace();
            return "Error updating job: " + e.getMessage();
        }
    }

    private JobDetail createJobDetail(JobRequest jobRequest) {
        return JobBuilder.newJob(JobProcess.class)
                .withIdentity(jobRequest.getJobName(), jobRequest.getJobGroup())
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    private Trigger createTrigger(JobRequest jobRequest) {
        String triggerName = jobRequest.getJobName() + "_trigger";

        return switch (jobRequest.getScheduleType()) {
            case CRON -> TriggerBuilder.newTrigger()
                    .withIdentity(triggerName, jobRequest.getJobGroup())
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobRequest.getCronExpression()))
                    .forJob(jobRequest.getJobName(), jobRequest.getJobGroup())
                    .build();
            case FIXED_DELAY -> TriggerBuilder.newTrigger()
                    .withIdentity(triggerName, jobRequest.getJobGroup())
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMilliseconds(jobRequest.getFixedDelay())
                            .repeatForever())
                    .forJob(jobRequest.getJobName(), jobRequest.getJobGroup())
                    .build();
            case FIXED_RATE -> TriggerBuilder.newTrigger()
                    .withIdentity(triggerName, jobRequest.getJobGroup())
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMilliseconds(jobRequest.getFixedRate())
                            .repeatForever())
                    .forJob(jobRequest.getJobName(), jobRequest.getJobGroup())
                    .build();
            case ONE_TIME -> TriggerBuilder.newTrigger()
                    .withIdentity(triggerName, jobRequest.getJobGroup())
                    .startAt(jobRequest.getStartTime())
                    .forJob(jobRequest.getJobName(), jobRequest.getJobGroup())
                    .build();
            default -> throw new IllegalArgumentException("Invalid schedule type: " + jobRequest.getScheduleType());
        };
    }


}
