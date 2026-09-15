package com.codeit.modoo_playlist.modulebatch.embedding.scheduler;

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
@ConditionalOnProperty(prefix = "batch.embedding", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ContentEmbeddingBatchScheduler {

  private final JobOperator jobOperator;
  private final JobRepository jobRepository;

  private final Job contentEmbeddingJob;

  public ContentEmbeddingBatchScheduler(
      JobOperator jobOperator,
      JobRepository jobRepository,
      @Qualifier("contentEmbeddingJob") Job contentEmbeddingJob
  ) {
    this.jobOperator = jobOperator;
    this.jobRepository = jobRepository;
    this.contentEmbeddingJob = contentEmbeddingJob;
  }

  @Scheduled(cron = "${batch.embedding.cron:0 0 23 * * *}", zone = "${batch.embedding.zone:Asia/Seoul}")
  public void run() throws Exception {
    Set<JobExecution> staleExecutions = jobRepository.findRunningJobExecutions(contentEmbeddingJob.getName());
    for (JobExecution staleExecution : staleExecutions) {
      log.warn("콘텐츠 임베딩 Job이 STARTED 상태로 남아있어 비정상 종료로 보고 복구합니다. jobExecutionId={}",
          staleExecution.getId());
      jobOperator.recover(staleExecution);
    }

    String scheduleSlot = Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
    try {
      jobOperator.start(
          contentEmbeddingJob,
          new JobParametersBuilder()
              .addString("scheduleSlot", scheduleSlot)
              .toJobParameters()
      );
    } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException exception) {
      log.info("같은 스케줄 구간의 콘텐츠 임베딩이 이미 실행됐습니다: {}", scheduleSlot);
    }
  }
}
