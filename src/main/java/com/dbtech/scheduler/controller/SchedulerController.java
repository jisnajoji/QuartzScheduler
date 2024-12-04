package com.dbtech.scheduler.controller;

import com.dbtech.scheduler.dto.request.JobRequest;
import com.dbtech.scheduler.dto.response.JobDetailDto;
import com.dbtech.scheduler.services.scheduler.SchedulerService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Scheduler controller.
 */
@RestController
@RequestMapping(value = "/jobs")
public class SchedulerController {

    private static final Logger log = LoggerFactory.getLogger(SchedulerController.class);

    @Autowired
    SchedulerService schedulerService;

    @PostMapping("/schedule")
    public String scheduleJob(@RequestBody JobRequest jobRequest) {
        return schedulerService.scheduleJob(jobRequest);
    }

    @GetMapping("/list")
    public List<JobDetailDto> listJobs() {
        return schedulerService.listJobs();
    }


    @DeleteMapping("/delete/{jobName}/{jobGroup}")
    public String deleteJob(@PathVariable String jobName, @PathVariable String jobGroup) {
        return schedulerService.deletejob(jobName, jobGroup);
    }

    @PostMapping("/pause/{jobName}/{jobGroup}")
    public String pauseJob(@PathVariable String jobName, @PathVariable String jobGroup) {
        return schedulerService.pauseJob(jobName, jobGroup);
    }

    @PostMapping("/restart/{jobName}/{jobGroup}")
    public String restartJob(@PathVariable String jobName, @PathVariable String jobGroup) {
        return schedulerService.restartJob(jobName, jobGroup);
    }

    @PutMapping("/update/{jobName}")
    public String updateJob(@PathVariable String jobName, @RequestBody JobRequest jobRequest) {
        return schedulerService.updateJob(jobName, jobRequest);
    }

}
