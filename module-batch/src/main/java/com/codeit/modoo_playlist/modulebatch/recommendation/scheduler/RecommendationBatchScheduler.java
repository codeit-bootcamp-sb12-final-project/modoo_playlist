package com.codeit.modoo_playlist.modulebatch.recommendation.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
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

@Component
@Slf4j
@ConditionalOnProperty(prefix = "batch.recommendation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecommendationBatchScheduler {

  private final JobOperator jobOperator;
  private final JobRepository jobRepository;

  private final Job nightlyRecalcJob;

  public RecommendationBatchScheduler(
      JobOperator jobOperator,
      JobRepository jobRepository,
      @Qualifier("nightlyRecalcJob") Job nightlyRecalcJob
  ) {
    this.jobOperator = jobOperator;
    this.jobRepository = jobRepository;
    this.nightlyRecalcJob = nightlyRecalcJob;
  }

  @Scheduled(cron = "${batch.recommendation.cron:0 30 0 * * *}", zone = "${batch.recommendation.zone:Asia/Seoul}")
  public void run() throws Exception {
    if (!jobRepository.findRunningJobExecutions(nightlyRecalcJob.getName()).isEmpty()) {
      log.warn("추천 야간 재계산 Job이 이미 실행 중이므로 이번 스케줄을 건너뜁니다.");
      return;
    }

    String scheduleSlot = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
    try {
      jobOperator.start(
          nightlyRecalcJob,
          new JobParametersBuilder()
              .addString("scheduleSlot", scheduleSlot)
              .toJobParameters()
      );
    } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException exception) {
      log.info("같은 스케줄 구간의 추천 재계산이 이미 실행됐습니다: {}", scheduleSlot);
    }
  }
}
