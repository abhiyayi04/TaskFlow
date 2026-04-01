package com.taskflow.dto;

import com.taskflow.model.Job;
import com.taskflow.model.JobStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobResponse {

    private String id;
    private String type;
    private String payload;
    private JobStatus status;
    private LocalDateTime scheduledAt;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JobResponse from(Job job) {
        JobResponse r = new JobResponse();
        r.id = job.getId();
        r.type = job.getType();
        r.payload = job.getPayload();
        r.status = job.getStatus();
        r.scheduledAt = job.getScheduledAt();
        r.retryCount = job.getRetryCount();
        r.maxRetries = job.getMaxRetries();
        r.createdAt = job.getCreatedAt();
        r.updatedAt = job.getUpdatedAt();
        return r;
    }
}
