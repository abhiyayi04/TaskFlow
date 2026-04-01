package com.taskflow.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "workers")
@Data
public class Worker {

    @Id
    @Column(name = "worker_id", nullable = false)
    private String workerId;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;
}
