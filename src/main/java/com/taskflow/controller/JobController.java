package com.taskflow.controller;

import com.taskflow.dto.CreateJobRequest;
import com.taskflow.dto.JobResponse;
import com.taskflow.model.JobStatus;
import com.taskflow.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job management API")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a new job")
    public JobResponse createJob(@RequestBody CreateJobRequest request) {
        return JobResponse.from(jobService.createJob(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job status by ID")
    public JobResponse getJob(@PathVariable String id) {
        return JobResponse.from(jobService.getJob(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a queued or running job")
    public void cancelJob(@PathVariable String id) {
        jobService.cancelJob(id);
    }

    @GetMapping
    @Operation(summary = "List jobs, optionally filtered by status and/or type")
    public List<JobResponse> listJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) String type) {
        return jobService.listJobs(status, type).stream()
                .map(JobResponse::from)
                .toList();
    }
}
