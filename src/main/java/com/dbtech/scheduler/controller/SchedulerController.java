package com.dbtech.scheduler.controller;

import com.dbtech.scheduler.entity.Job;
import com.dbtech.scheduler.services.SchedulerService;
import java.util.List;

import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Scheduler controller.
 */
@RestController
public class SchedulerController {

    private static final Logger log = LoggerFactory.getLogger(SchedulerController.class);

    @Autowired
    SchedulerService schedulerService;

    @PostMapping("/schedule")
    public void scheduleJob(@RequestBody Job job){
        if(null!=job){
            schedulerService.scheduleJob(job);
        }
    }
    
    @GetMapping("/scheduledJobs")
    public List<Job> getScheduledJobs() {
        return schedulerService.getScheduledJobs();
    }

    @PutMapping("/updateJob/{id}")
    public ResponseEntity<String> updateScheduledJob(@PathVariable Long id, @RequestBody Job job) {
        try {
            schedulerService.updateScheduledJob(id, job);
            return ResponseEntity.ok("Job with ID" + id + "has been updated.");
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request body");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error in updating job with ID" + id);
        }
    }
    
    @DeleteMapping("/deleteJob/{id}")
    public void deleteScheduledJob(@PathVariable Long id){
        schedulerService.deleteScheduledJob(id);
    }

    @PostMapping("/restart/{id}")
    public ResponseEntity<String> restartJob(@PathVariable Long id) {
        try {
            schedulerService.restartJob(id);
            return ResponseEntity.ok("Job with ID " + id + " has been restarted.");
        } catch (Exception e) {
            log.error("Error restarting job with ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error restarting job with ID " + id + ": " + e.getMessage());
        }
    }

    @PostMapping("/stop/{id}")
    public ResponseEntity<String> stopJob(@PathVariable Long id) {
        try {
            schedulerService.stopJob(id);
            return ResponseEntity.ok("Job with ID " + id + " has been stopped.");
        } catch (Exception e) {
            log.error("Error stopping job with ID: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error stopping job with ID " + id + ": " + e.getMessage());
        }
    }
}
