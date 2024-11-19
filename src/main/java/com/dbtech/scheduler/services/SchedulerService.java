package com.dbtech.scheduler.services;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Scheduler service.
 */
@Service
@Slf4j
public class SchedulerService {

    private TaskScheduler taskScheduler;
    private ScheduledExecutorService executorService;
    private ConcurrentHashMap<String, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ScheduledExecutorService> jobExecutors = new ConcurrentHashMap<>();
    private final JobRepository jobRepository;

    @Autowired
    public SchedulerService(TaskScheduler taskScheduler, WebClient webClient, JobRepository jobRepository) {
        this.taskScheduler = taskScheduler;
        this.jobRepository = jobRepository;
    }

    @PostConstruct
    public void initializeScheduledTasks() {
        List<Job> activeJobs = jobRepository.findByIsStoppedFalse();
        log.info("Reinitializing " + activeJobs.size() + " active jobs.");
        for (Job job : activeJobs) {
            scheduleJob(job);
        }
    }

    public List<Job> getScheduledJobs(){
        return jobRepository.findAll();
    }

    public void deleteScheduledJob(Long id) {
        Job job = getJobById(id);
        jobRepository.delete(job);
        log.info("Deleted job with ID: " + id);
    }

    public Job getJobById(Long id) {
        Optional<Job> job = jobRepository.findById(id);
        if (job.isEmpty()) {
            log.error("No job exists with ID: " + id);
            throw new EntityNotFoundException("No job exists with the given id " + id);
        } else {
            return job.get();
        }
    }

    public void restartJob(Long id) {
        Job job = getJobById(id);
        if (!job.getIsStopped()) {
            log.warn("Job is already active: " + id);
            return;
        }
        log.info("Restarting job with ID: " + id);
        job.setIsStopped(Boolean.FALSE);
        jobRepository.save(job);
        scheduleJob(job);
    }

    public void stopJob(Long id) {
        // Retrieve the job by its ID
        Job job = getJobById(id);
        if (job == null) {
            throw new IllegalArgumentException("Job not found with ID: " + id);
        }

        // Set the job's 'isStopped' flag to true to indicate it should not run further
        job.setIsStopped(Boolean.TRUE);
        jobRepository.save(job); // Persist the change to the database
        log.info("Job with ID " + id + " has been stopped.");

        executorService = jobExecutors.get(job.getJobId());
        if (executorService != null) {
            executorService.shutdownNow();
            log.info("Executor for Job ID " + job.getJobId() + " has been stopped.");
            jobExecutors.remove(job.getJobId()); // Remove the executor from the map
        }

        // Check if the job has an associated cron task and cancel it
        if (scheduledJobs.containsKey(job.getJobId())) {
            ScheduledFuture<?> scheduledTask = scheduledJobs.get(job.getJobId());
            if (scheduledTask != null) {
                scheduledTask.cancel(true); // Cancel the cron job
                log.info("Cron job with ID " + job.getJobId() + " has been canceled.");
            }
        }
    }

    public void scheduleJob(Job job) {
        log.info("Scheduling job: " + job.toString());

        // Generate jobId if not already set
        if (job.getJobId() == null) {
            String jobId = job.getJobName() + "-" + System.currentTimeMillis();
            job.setJobId(jobId);
            job.setIsStopped(Boolean.FALSE);
        }

        // Validate input
        if ((job.getCron() == null || job.getCron().isBlank()) && (job.getDelayInMillis() == null || job.getDelayInMillis() <= 0)) {
            throw new IllegalArgumentException("Either 'cron' or 'delayInMillis' must be provided.");
        }

        // Use ScheduledExecutorService for delay
        if (job.getCron() == null || job.getCron().isBlank()) {
            log.info("Scheduling job with delay (ms): " + job.getDelayInMillis());

            executorService = Executors.newSingleThreadScheduledExecutor();

            jobExecutors.put(job.getJobId(), executorService);

            executorService.scheduleAtFixedRate(() -> {
                if (job.getIsStopped() != null && job.getIsStopped()) {
                    log.info("Job " + job.getJobName() + " is stopped. Exiting execution.");
                    executorService.shutdownNow(); // Cancel any further tasks
                    return;
                }

                log.info("Scheduler is on: Fixed rate job: " + job.getJobId() + " time of execution: " + LocalDateTime.now());
            }, job.getDelayInMillis(), job.getDelayInMillis(), TimeUnit.MILLISECONDS);

        } else {
            // Schedule using cron
            log.info("Scheduling job with cron expression: " + job.getCron());

            ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(() -> {
                if (job.getIsStopped() != null && job.getIsStopped()) {
                    log.info("Job " + job.getJobName() + " is stopped. Exiting execution.");
                    return; // Stop execution if job is stopped
                }

                log.info("Scheduler is on: Cron job: " + job.getJobId() + " time of execution: " + LocalDateTime.now());
            }, new CronTrigger(job.getCron(), TimeZone.getTimeZone(TimeZone.getDefault().toZoneId())));

            // Store the scheduled task to allow for cancellation later
            scheduledJobs.put(job.getJobId(), scheduledFuture);
        }
        jobRepository.save(job);
    }

    public void updateScheduledJob(Long id, Job job) throws BadRequestException {
        if (job.getCron() != null && job.getDelayInMillis() != null) {
            throw new BadRequestException("Either cron or delay is allowed");
        }
        Job jobTobeUpdated = getJobById(id);
        Boolean stoppedOrNot = jobTobeUpdated.getIsStopped();
        if (!stoppedOrNot) { // executing job is getting stopped before updating
            stopJob(id);
        }
        Job jobUpdated = updateJobDetails(jobTobeUpdated, job, stoppedOrNot);
        if (!jobUpdated.getIsStopped()) { //  if job was running before updation, restarting the job.
            scheduleJob(jobUpdated);
        }
    }

    private Job updateJobDetails(Job jobTobeUpdated, Job job, Boolean stoppedOrNot) {
        jobTobeUpdated.setJobName(job.getJobName());
        jobTobeUpdated.setCron(job.getCron());
        jobTobeUpdated.setDelayInMillis(job.getDelayInMillis());
        jobTobeUpdated.setIsStopped(stoppedOrNot);
        String jobId = jobTobeUpdated.getJobName() + "-" + System.currentTimeMillis();
        jobTobeUpdated.setJobId(jobId);
        return jobRepository.save(jobTobeUpdated);
    }
}
