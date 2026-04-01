package com.taskflow.worker;

import com.taskflow.model.Worker;
import com.taskflow.repository.WorkerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerHeartbeatService {

    private final WorkerRepository workerRepository;

    private final String workerId = UUID.randomUUID().toString();

    @PostConstruct
    public void register() {
        Worker worker = new Worker();
        worker.setWorkerId(workerId);
        worker.setLastHeartbeat(LocalDateTime.now());
        workerRepository.save(worker);
        log.info("Worker registered id={}", workerId);
    }

    @Scheduled(fixedRate = 5000)
    public void heartbeat() {
        Worker worker = workerRepository.findById(workerId).orElseGet(() -> {
            Worker w = new Worker();
            w.setWorkerId(workerId);
            return w;
        });
        worker.setLastHeartbeat(LocalDateTime.now());
        workerRepository.save(worker);
        log.debug("Heartbeat updated for worker id={}", workerId);
    }

    public String getWorkerId() {
        return workerId;
    }
}
