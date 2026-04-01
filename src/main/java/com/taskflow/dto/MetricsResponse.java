package com.taskflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricsResponse {
    private long totalProcessed;
    private long totalFailed;
    private long totalRetried;
}
