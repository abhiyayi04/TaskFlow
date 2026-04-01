package com.taskflow.worker;

import com.taskflow.model.Job;
import com.taskflow.model.JobStatus;
import com.taskflow.repository.JobAttemptRepository;
import com.taskflow.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobExecutionServiceTest {

    @Mock
    JobRepository jobRepository;

    @Mock
    JobAttemptRepository jobAttemptRepository;

    @Mock
    WorkerRegistry workerRegistry;

    @Mock
    JobHandler handler;

    @InjectMocks
    JobExecutionService jobExecutionService;

    private Job job;

    @BeforeEach
    void setUp() {
        job = new Job();
        job.setId("test-job-id");
        job.setType("email");
        job.setStatus(JobStatus.RUNNING);
        job.setRetryCount(0);
        job.setMaxRetries(3);

        when(jobRepository.findById("test-job-id")).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void execute_completesJob_whenHandlerSucceeds() throws Exception {
        when(workerRegistry.getHandler("email")).thenReturn(Optional.of(handler));
        doNothing().when(handler).handle(job);

        jobExecutionService.execute(job);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeastOnce()).save(captor.capture());
        List<Job> saved = captor.getAllValues();
        assertThat(saved.getLast().getStatus()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void execute_recordsAttempt_onSuccess() throws Exception {
        when(workerRegistry.getHandler("email")).thenReturn(Optional.of(handler));
        doNothing().when(handler).handle(job);

        jobExecutionService.execute(job);

        verify(jobAttemptRepository).save(argThat(attempt ->
                attempt.getStatus() == JobStatus.COMPLETED &&
                attempt.getAttemptNumber() == 1
        ));
    }

    @Test
    void execute_requeuesJob_whenHandlerFailsAndRetriesRemain() throws Exception {
        when(workerRegistry.getHandler("email")).thenReturn(Optional.of(handler));
        doThrow(new RuntimeException("timeout")).when(handler).handle(any());

        jobExecutionService.execute(job);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeastOnce()).save(captor.capture());
        Job finalState = captor.getAllValues().getLast();
        assertThat(finalState.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(finalState.getRetryCount()).isEqualTo(1);
    }

    @Test
    void execute_recordsAttempt_onFailureBeforeRetry() throws Exception {
        when(workerRegistry.getHandler("email")).thenReturn(Optional.of(handler));
        doThrow(new RuntimeException("timeout")).when(handler).handle(any());

        jobExecutionService.execute(job);

        verify(jobAttemptRepository).save(argThat(attempt ->
                attempt.getStatus() == JobStatus.FAILED &&
                attempt.getErrorMessage().equals("timeout")
        ));
    }

    @Test
    void execute_movesJobToDLQ_whenRetriesExhausted() throws Exception {
        job.setRetryCount(3);
        job.setMaxRetries(3);
        when(workerRegistry.getHandler("email")).thenReturn(Optional.of(handler));
        doThrow(new RuntimeException("fatal")).when(handler).handle(any());

        jobExecutionService.execute(job);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    void execute_doesNotRetry_whenRetriesExhausted() throws Exception {
        job.setRetryCount(3);
        job.setMaxRetries(3);
        when(workerRegistry.getHandler("email")).thenReturn(Optional.of(handler));
        doThrow(new RuntimeException("fatal")).when(handler).handle(any());

        jobExecutionService.execute(job);

        // retryCount must not be incremented past maxRetries
        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getRetryCount()).isEqualTo(3);
    }

    @Test
    void execute_failsImmediately_whenNoHandlerRegistered() throws Exception {
        when(workerRegistry.getHandler("email")).thenReturn(Optional.empty());

        jobExecutionService.execute(job);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getStatus()).isEqualTo(JobStatus.FAILED);
        verify(handler, never()).handle(any());
    }
}
