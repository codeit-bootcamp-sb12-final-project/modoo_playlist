package com.codeit.modoo_playlist.modulebatch.user.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
class UserDeletionBatchSchedulerTest {

  @Mock
  private JobOperator jobOperator;

  @Mock
  private JobRepository jobRepository;

  @Mock
  private Job userDeletionJob;

  @Test
  void calculatesCutoffFromLocalMidnight() {
    Instant now = Instant.parse("2026-09-22T11:00:00Z");

    Instant cutoff = UserDeletionBatchScheduler.calculateCutoff(
        now,
        Duration.ofDays(1),
        ZoneId.of("Asia/Seoul")
    );

    assertThat(cutoff).isEqualTo(Instant.parse("2026-09-20T15:00:00Z"));
  }

  @Test
  void startsJobWithFixedDeletionDateAndCutoff() throws Exception {
    when(userDeletionJob.getName()).thenReturn("userDeletionJob");
    when(jobRepository.findRunningJobExecutions("userDeletionJob")).thenReturn(Set.of());
    UserDeletionBatchScheduler scheduler = new UserDeletionBatchScheduler(
        jobOperator,
        jobRepository,
        userDeletionJob,
        Duration.ofDays(1),
        "Asia/Seoul"
    );
    Instant now = Instant.parse("2026-09-22T11:00:00Z");

    try (MockedStatic<Instant> instantMock = mockStatic(Instant.class,
        Answers.CALLS_REAL_METHODS)) {
      instantMock.when(Instant::now).thenReturn(now);
      scheduler.run();
    }

    ArgumentCaptor<JobParameters> parameters = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobOperator).start(eq(userDeletionJob), parameters.capture());
    assertThat(parameters.getValue().getString("deletionDate")).isEqualTo("2026-09-22");
    assertThat(parameters.getValue().getString("deletionCutoff"))
        .isEqualTo("2026-09-20T15:00:00Z");
  }
}
