package com.taskflow.service;

import com.taskflow.dto.CreateJobRequest;
import com.taskflow.model.Job;
import com.taskflow.model.JobStatus;
import com.taskflow.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;

    public Job createJob(CreateJobRequest request) {
        Job job = new Job();
        job.setType(request.getType());
        job.setPayload(request.getPayload());
        job.setStatus(JobStatus.QUEUED);
        job.setScheduledAt(request.getScheduledAt());
        job.setMaxRetries(request.getMaxRetries());
        Job saved = jobRepository.save(job);
        log.info("Job submitted id={} type={} scheduledAt={}", saved.getId(), saved.getType(), saved.getScheduledAt());
        return saved;
    }

    public Job getJob(String id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found: " + id));
    }

    public void cancelJob(String id) {
        Job job = getJob(id);
        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot cancel a job with status: " + job.getStatus());
        }
        job.setStatus(JobStatus.FAILED);
        jobRepository.save(job);
        log.info("Job cancelled id={}", id);
    }

    public List<Job> listJobs(JobStatus status, String type) {
        if (status != null && type != null) {
            return jobRepository.findByStatusAndType(status, type);
        } else if (status != null) {
            return jobRepository.findByStatus(status);
        } else if (type != null) {
            return jobRepository.findByType(type);
        }
        return jobRepository.findAll();
    }
}
