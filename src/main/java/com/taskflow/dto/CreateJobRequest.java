package com.taskflow.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateJobRequest {

    private String type;
    private String payload;
    private LocalDateTime scheduledAt;
    private int maxRetries = 3;
}
