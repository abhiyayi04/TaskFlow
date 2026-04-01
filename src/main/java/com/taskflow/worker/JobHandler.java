package com.taskflow.worker;

import com.taskflow.model.Job;

public interface JobHandler {

    String getType();

    void handle(Job job) throws Exception;
}
