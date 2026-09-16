package com.codeit.modoo_playlist.modulebatch.monitoring;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchMetricsListener implements JobExecutionListener {

    private static final String EXECUTIONS_METRIC = "modoo.batch.job.executions";
    private static final String DURATION_METRIC = "modoo.batch.job.duration";
    private static final String ITEMS_METRIC = "modoo.batch.job.items";

    private final MeterRegistry meterRegistry;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(
                "배치 Job을 시작합니다. job={}, executionId={}",
                jobName(jobExecution),
                jobExecution.getId()
        );
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobName(jobExecution);
        String status = jobExecution.getStatus().name();
        long durationMillis = durationMillis(jobExecution);
        long readCount = sum(jobExecution, ItemCount.READ);
        long writeCount = sum(jobExecution, ItemCount.WRITE);
        long skipCount = sum(jobExecution, ItemCount.SKIP);

        Counter.builder(EXECUTIONS_METRIC)
                .description("배치 Job 실행 횟수")
                .tag("job", jobName)
                .tag("status", status)
                .register(meterRegistry)
                .increment();

        Timer.builder(DURATION_METRIC)
                .description("배치 Job 실행 시간")
                .tag("job", jobName)
                .tag("status", status)
                .register(meterRegistry)
                .record(durationMillis, TimeUnit.MILLISECONDS);

        recordItems(jobName, "read", readCount);
        recordItems(jobName, "write", writeCount);
        recordItems(jobName, "skip", skipCount);

        if (jobExecution.getStatus().isUnsuccessful()) {
            Throwable failure = jobExecution.getAllFailureExceptions().stream()
                    .findFirst()
                    .orElse(null);
            log.error(
                    "배치 Job이 실패했습니다. job={}, executionId={}, status={}, durationMs={}, "
                            + "read={}, write={}, skip={}, reason={}",
                    jobName,
                    jobExecution.getId(),
                    status,
                    durationMillis,
                    readCount,
                    writeCount,
                    skipCount,
                    failure == null ? null : failure.getMessage(),
                    failure
            );
            return;
        }

        log.info(
                "배치 Job이 완료됐습니다. job={}, executionId={}, status={}, durationMs={}, "
                        + "read={}, write={}, skip={}",
                jobName,
                jobExecution.getId(),
                status,
                durationMillis,
                readCount,
                writeCount,
                skipCount
        );
    }

    private void recordItems(String jobName, String type, long count) {
        Counter.builder(ITEMS_METRIC)
                .description("배치 Job 처리 건수")
                .tag("job", jobName)
                .tag("type", type)
                .register(meterRegistry)
                .increment(count);
    }

    private long durationMillis(JobExecution jobExecution) {
        if (jobExecution.getStartTime() == null || jobExecution.getEndTime() == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(
                jobExecution.getStartTime(),
                jobExecution.getEndTime()
        ).toMillis());
    }

    private long sum(JobExecution jobExecution, ItemCount itemCount) {
        return jobExecution.getStepExecutions().stream()
                .mapToLong(stepExecution -> itemCount.value(stepExecution))
                .sum();
    }

    private String jobName(JobExecution jobExecution) {
        return jobExecution.getJobInstance().getJobName();
    }

    private enum ItemCount {
        READ {
            @Override
            long value(StepExecution stepExecution) {
                return stepExecution.getReadCount();
            }
        },
        WRITE {
            @Override
            long value(StepExecution stepExecution) {
                return stepExecution.getWriteCount();
            }
        },
        SKIP {
            @Override
            long value(StepExecution stepExecution) {
                return stepExecution.getSkipCount();
            }
        };

        abstract long value(StepExecution stepExecution);
    }
}
