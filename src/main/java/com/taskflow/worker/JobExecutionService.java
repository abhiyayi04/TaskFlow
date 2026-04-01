package com.taskflow.worker;

import com.taskflow.model.Job;
import com.taskflow.model.JobAttempt;
import com.taskflow.model.JobStatus;
import com.taskflow.repository.JobAttemptRepository;
import com.taskflow.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobRepository jobRepository;
    private final JobAttemptRepository jobAttemptRepository;
    private final WorkerRegistry workerRegistry;

    @Transactional
    public void execute(Job job) {
        // Re-fetch inside transaction to get a managed entity
        job = jobRepository.findById(job.getId()).orElseThrow();

        JobHandler handler = workerRegistry.getHandler(job.getType()).orElse(null);

        if (handler == null) {
            log.warn("No handler for type '{}', marking failed. id={}", job.getType(), job.getId());
            recordAttempt(job, JobStatus.FAILED, "No handler for type: " + job.getType());
            markFailed(job);
            return;
        }

        try {
            handler.handle(job);
            recordAttempt(job, JobStatus.COMPLETED, null);
            markCompleted(job);
        } catch (Exception e) {
            log.error("Job failed id={} attempt={} error={}", job.getId(), job.getRetryCount() + 1, e.getMessage());
            recordAttempt(job, JobStatus.FAILED, e.getMessage());

            if (job.getRetryCount() < job.getMaxRetries()) {
                retry(job);
            } else {
                log.warn("Job exhausted retries, sending to DLQ. id={}", job.getId());
                markFailed(job);
            }
        }
    }

    private void recordAttempt(Job job, JobStatus status, String errorMessage) {
        JobAttempt attempt = new JobAttempt();
        attempt.setJob(job);
        attempt.setAttemptNumber(job.getRetryCount() + 1);
        attempt.setStatus(status);
        attempt.setErrorMessage(errorMessage);
        jobAttemptRepository.save(attempt);
    }

    private void markCompleted(Job job) {
        job.setStatus(JobStatus.COMPLETED);
        jobRepository.save(job);
        log.info("Job completed id={}", job.getId());
    }

    private void retry(Job job) {
        job.setRetryCount(job.getRetryCount() + 1);
        job.setStatus(JobStatus.QUEUED);
        jobRepository.save(job);
        log.info("Job re-queued for retry id={} attempt={}/{}", job.getId(), job.getRetryCount(), job.getMaxRetries());
    }

    private void markFailed(Job job) {
        job.setStatus(JobStatus.FAILED);
        jobRepository.save(job);
        log.warn("Job moved to DLQ id={}", job.getId());
    }
}
