package com.taskflow.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WorkerRegistry {

    private final Map<String, JobHandler> handlers;

    public WorkerRegistry(List<JobHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(JobHandler::getType, Function.identity()));
        log.info("Registered job handlers: {}", handlers.keySet());
    }

    public Optional<JobHandler> getHandler(String jobType) {
        return Optional.ofNullable(handlers.get(jobType));
    }
}
