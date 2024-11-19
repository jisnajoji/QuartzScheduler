package com.dbtech.scheduler.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Table(name = "scheduler_job")
public class Job {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;
    private String jobId;
    private String jobName;
//    private String apiURL;
//    private String baseURL;
    private String cron;
    private Long delayInMillis;
    private Boolean isStopped;
}
