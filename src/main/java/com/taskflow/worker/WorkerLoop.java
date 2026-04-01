package com.taskflow.worker;

import com.taskflow.model.Job;
import com.taskflow.queue.JobQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class WorkerLoop {

    private static final int WORKER_THREADS = 3;

    private final ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREADS);

    private final JobQueueService jobQueueService;
    private final JobExecutionService jobExecutionService;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        Optional<Job> jobOpt = jobQueueService.claimNextJob();
        if (jobOpt.isEmpty()) {
            return;
        }

        Job job = jobOpt.get();
        log.info("Dispatching job id={} type={} to thread pool", job.getId(), job.getType());
        executor.submit(() -> jobExecutionService.execute(job));
    }
}
