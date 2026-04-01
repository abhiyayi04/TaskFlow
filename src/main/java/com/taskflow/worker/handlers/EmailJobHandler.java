package com.taskflow.worker.handlers;

import com.taskflow.model.Job;
import com.taskflow.worker.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailJobHandler implements JobHandler {

    @Override
    public String getType() {
        return "email";
    }

    @Override
    public void handle(Job job) throws Exception {
        log.info("Sending email for job id={} payload={}", job.getId(), job.getPayload());
        // Simulate work
        Thread.sleep(500);
        log.info("Email sent for job id={}", job.getId());
    }
}
