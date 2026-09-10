package com.codeit.modoo_playlist.modulebatch.tmdb.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "batch.tmdb", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TmdbBatchScheduler {

    private final JobOperator jobOperator;
    private final JobRepository jobRepository;

    private final Job tmdbSyncJob;

    public TmdbBatchScheduler(
            JobOperator jobOperator,
            JobRepository jobRepository,
            @Qualifier("tmdbSyncJob") Job tmdbSyncJob
    ) {
        this.jobOperator = jobOperator;
        this.jobRepository = jobRepository;
        this.tmdbSyncJob = tmdbSyncJob;
    }

    @Scheduled(cron = "${batch.tmdb.cron:0 0 0,12 * * *}", zone = "${batch.tmdb.zone:Asia/Seoul}")
    public void run() throws Exception {
        if (!jobRepository.findRunningJobExecutions(tmdbSyncJob.getName()).isEmpty()) {
            log.warn("TMDB 동기화 Job이 이미 실행 중이므로 이번 스케줄을 건너뜁니다.");
            return;
        }

        String scheduleSlot = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
        try {
            jobOperator.start(
                    tmdbSyncJob,
                    new JobParametersBuilder()
                            .addString("scheduleSlot", scheduleSlot)
                            .toJobParameters()
            );
        } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException exception) {
            log.info("같은 스케줄 구간의 TMDB 동기화가 이미 실행됐습니다: {}", scheduleSlot);
        }
    }
}
