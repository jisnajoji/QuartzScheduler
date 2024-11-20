package com.dbtech.scheduler.services;

import com.dbtech.scheduler.dto.request.JobRequest;
import com.dbtech.scheduler.entity.Job;
import com.dbtech.scheduler.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

/**
 * Scheduler service.
 */
@Service
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {

    private TaskScheduler taskScheduler;
    private ConcurrentHashMap<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();
    private final JobRepository jobRepository;

    @Autowired
    public SchedulerServiceImpl(TaskScheduler taskScheduler, JobRepository jobRepository) {
        this.taskScheduler = taskScheduler;
        this.jobRepository = jobRepository;
    }

    @PostConstruct
    public void initializeScheduledTasks() {
        List<Job> activeJobs = jobRepository.findByIsStoppedFalse();
        log.info("Reinitializing " + activeJobs.size() + " active jobs.");
        for (Job job : activeJobs) {
            JobRequest jobRequest = JobRequest.builder()
                    .jobId(job.getJobId())
                    .jobName(job.getJobName())
                    .cron(job.getCron())
                    .delayInMillis(job.getDelayInMillis())
                    .isStopped(job.getIsStopped())
                    .build();
            scheduleJob(jobRequest);
        }
    }

    @Override
    public List<Job> getScheduledJobs(){
        return jobRepository.findAll();
    }

    @Override
    public void deleteScheduledJob(Long id) {
        Job job = getJobById(id);
        jobRepository.delete(job);
        log.info("Deleted job with ID: " + id);
    }

    @Override
    public Job getJobById(Long id) {
        Optional<Job> job = jobRepository.findById(id);
        if (job.isEmpty()) {
            log.error("No job exists with ID: " + id);
            throw new EntityNotFoundException("No job exists with the given id " + id);
        } else {
            return job.get();
        }
    }

    @Override
    public void restartJob(Long id) {
        Job job = getJobById(id);
        if (!job.getIsStopped()) {
            log.warn("Job is already active: " + id);
            return;
        }
        log.info("Restarting job with ID: " + id);
        job.setIsStopped(Boolean.FALSE);
        job = jobRepository.save(job);
        JobRequest jobRequest = JobRequest.builder()
                .jobId(job.getJobId())
                .jobName(job.getJobName())
                .cron(job.getCron())
                .delayInMillis(job.getDelayInMillis())
                .isStopped(job.getIsStopped())
                .build();
        scheduleJob(jobRequest);
    }

    @Override
    public void stopJob(Long id) {
        Job job = getJobById(id);
        if (job == null) {
            throw new IllegalArgumentException("Job not found with ID: " + id);
        }

        // Update job status
        job.setIsStopped(Boolean.TRUE);
        jobRepository.save(job);

        // Cancel associated scheduled task
        if (scheduledJobs.containsKey(job.getJobId())) {
            ScheduledFuture<?> scheduledTask = scheduledJobs.get(job.getJobId());
            if (scheduledTask != null) {
                scheduledTask.cancel(true);
                log.info("Scheduled task for Job ID " + job.getJobId() + " has been stopped.");
                scheduledJobs.remove(job.getJobId());
            }
        }
    }


    @Override
    public void scheduleJob(JobRequest jobRequest) {
        log.info("Scheduling job: " + jobRequest.toString());

        String persistRequestJobId = jobRequest.getJobId();
        Job newJob = new Job();

        // Generate jobId if not already set
        if (jobRequest.getJobId() == null) {
            String jobId = jobRequest.getJobName() + "-" + System.currentTimeMillis();
            newJob.setJobId(jobId);
            jobRequest.setJobId(newJob.getJobId());
            newJob.setIsStopped(Boolean.FALSE);
            newJob.setCron(jobRequest.getCron());
            newJob.setDelayInMillis(jobRequest.getDelayInMillis());
            newJob.setJobName(jobRequest.getJobName());
        }

        // Validate input
        if ((jobRequest.getCron() == null || jobRequest.getCron().isBlank()) &&
                (jobRequest.getDelayInMillis() == null || jobRequest.getDelayInMillis() <= 0)) {
            throw new IllegalArgumentException("Either 'cron' or 'delayInMillis' must be provided.");
        }

        if (jobRequest.getCron() != null && !jobRequest.getCron().isBlank()) {
            // Schedule using cron
            log.info("Scheduling job with cron expression: " + jobRequest.getCron());

            ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(() -> {
                if (Boolean.TRUE.equals(jobRequest.getIsStopped())) {
                    log.info("Job " + jobRequest.getJobName() + " is stopped. Exiting execution.");
                    return;
                }

                log.info("Scheduler is on: Cron job: " + jobRequest.getJobId() + " time of execution: " + LocalDateTime.now());
            }, new CronTrigger(jobRequest.getCron(), TimeZone.getTimeZone(TimeZone.getDefault().toZoneId())));

            scheduledJobs.put(jobRequest.getJobId(), scheduledFuture);

        } else {
            // Schedule with fixed delay
            log.info("Scheduling job with fixed delay: " + jobRequest.getDelayInMillis());

            ScheduledFuture<?> scheduledFuture = taskScheduler.scheduleWithFixedDelay(() -> {
                if (Boolean.TRUE.equals(jobRequest.getIsStopped())) {
                    log.info("Job " + jobRequest.getJobName() + " is stopped. Exiting execution.");
                    return;
                }

                log.info("Scheduler is on: Fixed delay job: " + jobRequest.getJobId() + " time of execution: " + LocalDateTime.now());
            }, jobRequest.getDelayInMillis());

            scheduledJobs.put(jobRequest.getJobId(), scheduledFuture);
        }
        if (persistRequestJobId == null) {
            jobRepository.save(newJob);
        }
    }

    @Override
    public void updateScheduledJob(Long id, JobRequest jobRequest) throws BadRequestException {
        if (jobRequest.getCron() != null && jobRequest.getDelayInMillis() != null) {
            throw new BadRequestException("Either cron or delay is allowed");
        }
        Job jobTobeUpdated = getJobById(id);
        Boolean stoppedOrNot = jobTobeUpdated.getIsStopped();
        if (!stoppedOrNot) { // executing job is getting stopped before updating
            stopJob(id);
        }
        Job jobUpdated = updateJobDetails(jobTobeUpdated, jobRequest, stoppedOrNot);
        if (!jobUpdated.getIsStopped()) { //  if job was running before updation, restarting the job.
            JobRequest request = JobRequest.builder()
                    .jobId(jobUpdated.getJobId())
                    .jobName(jobUpdated.getJobName())
                    .cron(jobUpdated.getCron())
                    .delayInMillis(jobUpdated.getDelayInMillis())
                    .build();
            scheduleJob(request);
        }
    }

    private Job updateJobDetails(Job jobTobeUpdated, JobRequest job, Boolean stoppedOrNot) {
        jobTobeUpdated.setJobName(job.getJobName());
        jobTobeUpdated.setCron(job.getCron());
        jobTobeUpdated.setDelayInMillis(job.getDelayInMillis());
        jobTobeUpdated.setIsStopped(stoppedOrNot);
        String jobId = jobTobeUpdated.getJobName() + "-" + System.currentTimeMillis();
        jobTobeUpdated.setJobId(jobId);
        return jobRepository.save(jobTobeUpdated);
    }
}
