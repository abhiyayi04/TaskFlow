package com.taskflow.service;

import com.taskflow.dto.MetricsResponse;
import com.taskflow.model.JobStatus;
import com.taskflow.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final JobRepository jobRepository;

    public MetricsResponse getMetrics() {
        long totalProcessed = jobRepository.countByStatus(JobStatus.COMPLETED);
        long totalFailed    = jobRepository.countByStatus(JobStatus.FAILED);
        long totalRetried   = jobRepository.sumRetryCount();
        return new MetricsResponse(totalProcessed, totalFailed, totalRetried);
    }
}
