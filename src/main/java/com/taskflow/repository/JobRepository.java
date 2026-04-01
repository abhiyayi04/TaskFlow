package com.taskflow.repository;

import com.taskflow.model.Job;
import com.taskflow.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findByStatus(JobStatus status);

    List<Job> findByType(String type);

    List<Job> findByStatusAndType(JobStatus status, String type);

    @Query(value = """
            SELECT * FROM jobs
            WHERE status = 'QUEUED'
              AND (scheduled_at IS NULL OR scheduled_at <= NOW())
            ORDER BY created_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Job> findNextAvailableJob();
}
