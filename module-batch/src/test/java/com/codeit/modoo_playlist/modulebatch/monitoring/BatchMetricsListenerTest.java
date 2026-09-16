package com.codeit.modoo_playlist.modulebatch.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.step.StepExecution;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class BatchMetricsListenerTest {

    @Test
    void 완료된_Job의_실행시간과_처리건수를_기록한다() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BatchMetricsListener listener = new BatchMetricsListener(registry);
        JobExecution execution = execution(BatchStatus.COMPLETED);

        listener.afterJob(execution);

        assertThat(registry.get("modoo.batch.job.executions")
                .tag("job", "testJob")
                .tag("status", "COMPLETED")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get("modoo.batch.job.duration")
                .tag("job", "testJob")
                .tag("status", "COMPLETED")
                .timer().totalTime(java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(2);
        assertItemCount(registry, "read", 10);
        assertItemCount(registry, "write", 8);
        assertItemCount(registry, "skip", 2);
    }

    @Test
    void 실패한_Job도_상태별_실행횟수를_기록한다() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        BatchMetricsListener listener = new BatchMetricsListener(registry);

        listener.afterJob(execution(BatchStatus.FAILED));

        assertThat(registry.get("modoo.batch.job.executions")
                .tag("job", "testJob")
                .tag("status", "FAILED")
                .counter().count()).isEqualTo(1);
    }

    private JobExecution execution(BatchStatus status) {
        JobInstance instance = mock(JobInstance.class);
        when(instance.getJobName()).thenReturn("testJob");

        StepExecution step = mock(StepExecution.class);
        when(step.getReadCount()).thenReturn(10L);
        when(step.getWriteCount()).thenReturn(8L);
        when(step.getSkipCount()).thenReturn(2L);

        JobExecution execution = mock(JobExecution.class);
        when(execution.getJobInstance()).thenReturn(instance);
        when(execution.getStatus()).thenReturn(status);
        when(execution.getStartTime()).thenReturn(LocalDateTime.of(2026, 9, 16, 10, 0));
        when(execution.getEndTime()).thenReturn(LocalDateTime.of(2026, 9, 16, 10, 0, 2));
        when(execution.getStepExecutions()).thenReturn(Set.of(step));
        when(execution.getAllFailureExceptions()).thenReturn(List.of());
        return execution;
    }

    private void assertItemCount(SimpleMeterRegistry registry, String type, double expected) {
        assertThat(registry.get("modoo.batch.job.items")
                .tag("job", "testJob")
                .tag("type", type)
                .counter().count()).isEqualTo(expected);
    }
}
