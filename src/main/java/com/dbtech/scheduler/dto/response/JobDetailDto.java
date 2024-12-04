package com.dbtech.scheduler.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JobDetailDto {
    private String jobName;
    private String jobGroup;
    private String triggerType;
    private String triggerExpression;
}
