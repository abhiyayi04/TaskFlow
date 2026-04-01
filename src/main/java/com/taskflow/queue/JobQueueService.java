package com.taskflow.queue;

import com.taskflow.model.Job;
import com.taskflow.model.JobStatus;
import com.taskflow.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobQueueService {

    private final JobRepository jobRepository;

    /**
     * Atomically claims the next available queued job by acquiring a row-level lock
     * (SELECT FOR UPDATE SKIP LOCKED). Concurrent workers will each claim a different
     * job — locked rows are skipped, never double-claimed.
     */
    @Transactional
    public Optional<Job> claimNextJob() {
        Optional<Job> jobOpt = jobRepository.findNextAvailableJob();
        if (jobOpt.isEmpty()) {
            return Optional.empty();
        }

        Job job = jobOpt.get();
        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);

        log.info("Claimed job id={} type={}", job.getId(), job.getType());
        return Optional.of(job);
    }
}
