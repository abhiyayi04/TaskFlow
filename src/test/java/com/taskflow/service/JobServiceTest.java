package com.taskflow.service;

import com.taskflow.dto.CreateJobRequest;
import com.taskflow.model.Job;
import com.taskflow.model.JobStatus;
import com.taskflow.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    JobRepository jobRepository;

    @InjectMocks
    JobService jobService;

    @Test
    void createJob_setsStatusToQueuedAndSaves() {
        CreateJobRequest request = new CreateJobRequest();
        request.setType("email");
        request.setPayload("{\"to\":\"test@example.com\"}");
        request.setMaxRetries(3);

        Job saved = new Job();
        saved.setId("abc-123");
        saved.setType("email");
        saved.setStatus(JobStatus.QUEUED);
        when(jobRepository.save(any(Job.class))).thenReturn(saved);

        Job result = jobService.createJob(request);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(captor.getValue().getType()).isEqualTo("email");
        assertThat(result.getId()).isEqualTo("abc-123");
    }

    @Test
    void createJob_withScheduledAt_setsScheduledAt() {
        LocalDateTime future = LocalDateTime.now().plusHours(1);
        CreateJobRequest request = new CreateJobRequest();
        request.setType("report");
        request.setScheduledAt(future);

        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        jobService.createJob(request);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getScheduledAt()).isEqualTo(future);
    }

    @Test
    void getJob_throwsNotFound_whenJobMissing() {
        when(jobRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJob("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Job not found");
    }

    @Test
    void getJob_returnsJob_whenFound() {
        Job job = new Job();
        job.setId("abc-123");
        when(jobRepository.findById("abc-123")).thenReturn(Optional.of(job));

        Job result = jobService.getJob("abc-123");
        assertThat(result.getId()).isEqualTo("abc-123");
    }

    @Test
    void cancelJob_setsStatusToFailed_whenQueued() {
        Job job = new Job();
        job.setId("abc-123");
        job.setStatus(JobStatus.QUEUED);
        when(jobRepository.findById("abc-123")).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        jobService.cancelJob("abc-123");

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    void cancelJob_throwsConflict_whenAlreadyCompleted() {
        Job job = new Job();
        job.setId("abc-123");
        job.setStatus(JobStatus.COMPLETED);
        when(jobRepository.findById("abc-123")).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.cancelJob("abc-123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void cancelJob_throwsConflict_whenAlreadyFailed() {
        Job job = new Job();
        job.setId("abc-123");
        job.setStatus(JobStatus.FAILED);
        when(jobRepository.findById("abc-123")).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.cancelJob("abc-123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    void listJobs_returnsAll_whenNoFilters() {
        when(jobRepository.findAll()).thenReturn(List.of(new Job(), new Job()));

        List<Job> result = jobService.listJobs(null, null);

        assertThat(result).hasSize(2);
        verify(jobRepository).findAll();
    }

    @Test
    void listJobs_filtersByStatus() {
        when(jobRepository.findByStatus(JobStatus.QUEUED)).thenReturn(List.of(new Job()));

        List<Job> result = jobService.listJobs(JobStatus.QUEUED, null);

        assertThat(result).hasSize(1);
        verify(jobRepository).findByStatus(JobStatus.QUEUED);
    }

    @Test
    void listJobs_filtersByType() {
        when(jobRepository.findByType("email")).thenReturn(List.of(new Job(), new Job()));

        List<Job> result = jobService.listJobs(null, "email");

        assertThat(result).hasSize(2);
        verify(jobRepository).findByType("email");
    }

    @Test
    void listJobs_filtersByStatusAndType() {
        when(jobRepository.findByStatusAndType(JobStatus.FAILED, "email")).thenReturn(List.of(new Job()));

        List<Job> result = jobService.listJobs(JobStatus.FAILED, "email");

        assertThat(result).hasSize(1);
        verify(jobRepository).findByStatusAndType(JobStatus.FAILED, "email");
    }
}
