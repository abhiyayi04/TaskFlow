package com.taskflow.repository;

import com.taskflow.model.JobAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobAttemptRepository extends JpaRepository<JobAttempt, Long> {

    List<JobAttempt> findByJobIdOrderByAttemptNumberAsc(String jobId);
}
