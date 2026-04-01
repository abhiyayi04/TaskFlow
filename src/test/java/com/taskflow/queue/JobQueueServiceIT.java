package com.taskflow.queue;

import com.taskflow.model.Job;
import com.taskflow.model.JobStatus;
import com.taskflow.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class JobQueueServiceIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    JobQueueService jobQueueService;

    @Autowired
    JobRepository jobRepository;

    @BeforeEach
    void clearJobs() {
        jobRepository.deleteAll();
    }

    @Test
    void claimNextJob_returnsEmpty_whenNoJobsExist() {
        Optional<Job> result = jobQueueService.claimNextJob();
        assertThat(result).isEmpty();
    }

    @Test
    void claimNextJob_claimsOldestQueuedJob() {
        Job job = queuedJob("email");
        jobRepository.save(job);

        Optional<Job> claimed = jobQueueService.claimNextJob();

        assertThat(claimed).isPresent();
        assertThat(claimed.get().getId()).isEqualTo(job.getId());
        assertThat(claimed.get().getStatus()).isEqualTo(JobStatus.RUNNING);
    }

    @Test
    void claimNextJob_updatesStatusToRunning_inDatabase() {
        Job job = queuedJob("email");
        jobRepository.save(job);

        jobQueueService.claimNextJob();

        Job fromDb = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(fromDb.getStatus()).isEqualTo(JobStatus.RUNNING);
    }

    @Test
    void claimNextJob_skipsJobsScheduledInFuture() {
        Job futureJob = queuedJob("email");
        futureJob.setScheduledAt(LocalDateTime.now().plusHours(1));
        jobRepository.save(futureJob);

        Optional<Job> result = jobQueueService.claimNextJob();

        assertThat(result).isEmpty();
    }

    @Test
    void claimNextJob_picksUpJobWhoseScheduledAtHasPassed() {
        Job pastJob = queuedJob("email");
        pastJob.setScheduledAt(LocalDateTime.now().minusSeconds(5));
        jobRepository.save(pastJob);

        Optional<Job> result = jobQueueService.claimNextJob();

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(pastJob.getId());
    }

    @Test
    void claimNextJob_claimsJobsInCreatedAtOrder() {
        Job first = queuedJob("email");
        Job second = queuedJob("report");
        jobRepository.save(first);
        jobRepository.save(second);

        Optional<Job> claimed = jobQueueService.claimNextJob();

        assertThat(claimed).isPresent();
        assertThat(claimed.get().getId()).isEqualTo(first.getId());
    }

    @Test
    void claimNextJob_returnsEmpty_afterAllJobsClaimed() {
        jobRepository.save(queuedJob("email"));

        jobQueueService.claimNextJob();
        Optional<Job> second = jobQueueService.claimNextJob();

        assertThat(second).isEmpty();
    }

    private Job queuedJob(String type) {
        Job job = new Job();
        job.setType(type);
        job.setStatus(JobStatus.QUEUED);
        job.setMaxRetries(3);
        return job;
    }
}
