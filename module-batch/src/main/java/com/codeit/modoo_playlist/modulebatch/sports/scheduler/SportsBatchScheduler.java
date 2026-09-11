package com.codeit.modoo_playlist.modulebatch.sports.scheduler;

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
@ConditionalOnProperty(prefix = "batch.sports", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SportsBatchScheduler {

    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final Job sportsSyncJob;

    public SportsBatchScheduler(
            JobOperator jobOperator,
            JobRepository jobRepository,
            @Qualifier("sportsSyncJob") Job sportsSyncJob
    ) {
        this.jobOperator = jobOperator;
        this.jobRepository = jobRepository;
        this.sportsSyncJob = sportsSyncJob;
    }

    @Scheduled(cron = "${batch.sports.cron:0 0 6,18 * * *}", zone = "${batch.sports.zone:Asia/Seoul}")
    public void run() throws Exception {
        if (!jobRepository.findRunningJobExecutions(sportsSyncJob.getName()).isEmpty()) {
            log.warn("SportsDB 동기화 Job이 이미 실행 중이므로 이번 스케줄을 건너뜁니다.");
            return;
        }

        String scheduleSlot = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
        try {
            jobOperator.start(
                    sportsSyncJob,
                    new JobParametersBuilder()
                            .addString("scheduleSlot", scheduleSlot)
                            .toJobParameters()
            );
        } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException exception) {
            log.info("같은 스케줄 구간의 SportsDB 동기화가 이미 실행됐습니다: {}", scheduleSlot);
        }
    }
}
