package com.codeit.modoo_playlist.modulebatch.user.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "batch.user-deletion",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class UserDeletionBatchScheduler {

  private final JobOperator jobOperator;
  private final JobRepository jobRepository;
  private final Job userDeletionJob;
  private final Duration retention;
  private final ZoneId deletionZone;

  public UserDeletionBatchScheduler(
      JobOperator jobOperator,
      JobRepository jobRepository,
      @Qualifier("userDeletionJob") Job userDeletionJob,
      @Value("${user-deletion.retention:1d}") Duration retention,
      @Value("${user-deletion.zone:Asia/Seoul}") String deletionZone
  ) {
    this.jobOperator = jobOperator;
    this.jobRepository = jobRepository;
    this.userDeletionJob = userDeletionJob;
    this.retention = retention;
    this.deletionZone = ZoneId.of(deletionZone);
  }

  @Scheduled(
      cron = "${batch.user-deletion.cron:0 0 0 * * *}",
      zone = "Asia/Seoul"
  )
  public void run() throws Exception {
//    복구 먼저 실행.
    Set<JobExecution> staleExecutions =
        jobRepository.findRunningJobExecutions(userDeletionJob.getName());
    for (JobExecution staleExecution : staleExecutions) {
      log.warn(
          "탈퇴 사용자 삭제 Job이 STARTED 상태로 남아 있어 비정상 종료로 보고 복구합니다: jobExecutionId={}",
          staleExecution.getId()
      );
      jobOperator.recover(staleExecution);
    }

    Instant now = Instant.now();
    LocalDate deletionDate = now.atZone(deletionZone).toLocalDate();
    Instant deletionCutoff = calculateCutoff(now, retention, deletionZone);

    try {
      jobOperator.start(
          userDeletionJob,
          new JobParametersBuilder()
              .addString("deletionDate", deletionDate.toString())
              .addString("deletionCutoff", deletionCutoff.toString())
              .toJobParameters()
      );
    } catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException exception) {
      log.info("같은 삭제 기준일의 탈퇴 사용자 삭제 Job이 이미 실행되었습니다: {}", deletionDate);
    }
  }

  static Instant calculateCutoff(Instant now, Duration retention, ZoneId zone) {
    return now.atZone(zone)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .minus(retention);
  }
}
