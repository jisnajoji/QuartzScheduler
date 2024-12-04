package com.dbtech.scheduler.services.executionstatus;

import com.dbtech.scheduler.entity.JobExecutionStatus;
import com.dbtech.scheduler.repository.JobExecutionStatusRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class JobExecutionStatusServiceImpl implements JobExecutionStatusService {

    @Autowired
    private JobExecutionStatusRepository jobExecutionStatusRepository;

    @Transactional
    @Override
    public void logJobExecution(String jobName, String jobGroup, boolean success, String failureReason) {
        JobExecutionStatus jobExecutionStatus = new JobExecutionStatus();
        jobExecutionStatus.setJobName(jobName);
        jobExecutionStatus.setJobGroup(jobGroup);
        jobExecutionStatus.setExecutionTime(LocalDateTime.now());
        jobExecutionStatus.setSuccess(success);
        jobExecutionStatus.setFailureReason(success ? null : failureReason);

        jobExecutionStatusRepository.save(jobExecutionStatus);
    }
}
