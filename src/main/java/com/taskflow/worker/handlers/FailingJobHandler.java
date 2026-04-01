package com.taskflow.worker.handlers;

import com.taskflow.model.Job;
import com.taskflow.worker.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FailingJobHandler implements JobHandler {

    @Override
    public String getType() {
        return "failing";
    }

    @Override
    public void handle(Job job) throws Exception {
        log.info("Executing failing job id={}", job.getId());
        throw new RuntimeException("Simulated failure for testing retry logic");
    }
}
