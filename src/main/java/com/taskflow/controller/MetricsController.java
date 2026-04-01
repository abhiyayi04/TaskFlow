package com.taskflow.controller;

import com.taskflow.dto.MetricsResponse;
import com.taskflow.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics", description = "Job processing metrics")
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping
    @Operation(summary = "Get aggregate job counts")
    public MetricsResponse getMetrics() {
        return metricsService.getMetrics();
    }
}
