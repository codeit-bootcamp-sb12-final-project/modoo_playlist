package com.codeit.modoo_playlist.modulebatch.reviewsummary.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
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
@ConditionalOnProperty(prefix = "batch.review-summary", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReviewSummaryBatchScheduler {

  private final JobOperator jobOperator;
  private final JobRepository jobRepository;

  private final Job reviewSummaryJob;

  public ReviewSummaryBatchScheduler(
      JobOperator jobOperator,
      JobRepository jobRepository,
      @Qualifier("reviewSummaryJob") Job reviewSummaryJob
  ) {
    this.jobOperator = jobOperator;
    this.jobRepository = jobRepository;
    this.reviewSummaryJob = reviewSummaryJob;
  }

  @Scheduled(cron = "${batch.review-summary.cron:0 30 1 * * *}", zone = "${batch.review-summary.zone:Asia/Seoul}")
  public void run() throws Exception {
    Set<JobExecution> staleExecutions = jobRepository.findRunningJobExecutions(reviewSummaryJob.getName());
    for (JobExecution staleExecution : staleExecutions) {
      log.warn("리뷰 요약 Job이 STARTED 상태로 남아있어 비정상 종료로 보고 복구합니다. jobExecutionId={}",
          staleExecution.getId());
      jobOperator.recover(staleExecution);
    }

    String scheduleSlot = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
    try {
      jobOperator.start(
          reviewSummaryJob,
          new JobParametersBuilder()
              .addString("scheduleSlot", scheduleSlot)
              .toJobParameters()
      );
    } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException exception) {
      log.info("같은 스케줄 구간의 리뷰 요약이 이미 실행됐습니다: {}", scheduleSlot);
    }
  }
}
