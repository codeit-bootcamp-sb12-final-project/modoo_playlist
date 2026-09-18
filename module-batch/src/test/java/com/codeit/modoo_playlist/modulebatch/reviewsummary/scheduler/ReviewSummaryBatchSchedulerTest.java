package com.codeit.modoo_playlist.modulebatch.reviewsummary.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryBatchSchedulerTest {

  @Mock private JobOperator jobOperator;
  @Mock private JobRepository jobRepository;
  @Mock private Job reviewSummaryJob;
  @Mock private JobExecution staleExecution;

  @Test
  void 실행중인_Job이_없으면_바로_새로_시작한다() throws Exception {
    when(reviewSummaryJob.getName()).thenReturn("reviewSummaryJob");
    when(jobRepository.findRunningJobExecutions("reviewSummaryJob")).thenReturn(Set.of());

    new ReviewSummaryBatchScheduler(jobOperator, jobRepository, reviewSummaryJob).run();

    verify(jobOperator, never()).recover(any());
    ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobOperator, times(1)).start(eq(reviewSummaryJob), captor.capture());
    Instant scheduleSlot = Instant.parse(captor.getValue().getString("scheduleSlot"));
    assertThat(scheduleSlot.getNano()).isZero();
    assertThat(scheduleSlot.getEpochSecond() % 60).isZero();
  }

  @Test
  void 같은_분_안에서_두번_실행해도_scheduleSlot은_동일하다() throws Exception {
    when(reviewSummaryJob.getName()).thenReturn("reviewSummaryJob");
    when(jobRepository.findRunningJobExecutions("reviewSummaryJob")).thenReturn(Set.of());
    ReviewSummaryBatchScheduler scheduler =
        new ReviewSummaryBatchScheduler(jobOperator, jobRepository, reviewSummaryJob);

    Instant fixedNow = Instant.parse("2026-01-01T00:00:30Z");
    try (MockedStatic<Instant> instantMock = mockStatic(Instant.class, Answers.CALLS_REAL_METHODS)) {
      instantMock.when(Instant::now).thenReturn(fixedNow);
      scheduler.run();
      scheduler.run();
    }

    ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobOperator, times(2)).start(eq(reviewSummaryJob), captor.capture());
    List<String> scheduleSlots = captor.getAllValues().stream()
        .map(params -> params.getString("scheduleSlot"))
        .toList();
    assertThat(scheduleSlots.get(0)).isEqualTo(scheduleSlots.get(1));
  }

  @Test
  void STARTED_상태로_남은_실행은_복구한_뒤_새로_시작한다() throws Exception {
    when(reviewSummaryJob.getName()).thenReturn("reviewSummaryJob");
    when(jobRepository.findRunningJobExecutions("reviewSummaryJob")).thenReturn(Set.of(staleExecution));

    new ReviewSummaryBatchScheduler(jobOperator, jobRepository, reviewSummaryJob).run();

    verify(jobOperator).recover(staleExecution);
    ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobOperator, times(1)).start(eq(reviewSummaryJob), captor.capture());
    Instant scheduleSlot = Instant.parse(captor.getValue().getString("scheduleSlot"));
    assertThat(scheduleSlot.getNano()).isZero();
    assertThat(scheduleSlot.getEpochSecond() % 60).isZero();
  }

  @Test
  void 같은_스케줄_구간에_이미_실행됐으면_예외를_삼킨다() throws Exception {
    when(reviewSummaryJob.getName()).thenReturn("reviewSummaryJob");
    when(jobRepository.findRunningJobExecutions("reviewSummaryJob")).thenReturn(Set.of());
    when(jobOperator.start(eq(reviewSummaryJob), any(JobParameters.class)))
        .thenThrow(new JobExecutionAlreadyRunningException("이미 실행 중"));

    assertThatCode(
        () -> new ReviewSummaryBatchScheduler(jobOperator, jobRepository, reviewSummaryJob).run()
    ).doesNotThrowAnyException();

    ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobOperator).start(eq(reviewSummaryJob), captor.capture());
    Instant scheduleSlot = Instant.parse(captor.getValue().getString("scheduleSlot"));
    assertThat(scheduleSlot.getNano()).isZero();
    assertThat(scheduleSlot.getEpochSecond() % 60).isZero();
  }

  @Test
  void 같은_스케줄_구간에_이미_완료됐으면_예외를_삼킨다() throws Exception {
    when(reviewSummaryJob.getName()).thenReturn("reviewSummaryJob");
    when(jobRepository.findRunningJobExecutions("reviewSummaryJob")).thenReturn(Set.of());
    when(jobOperator.start(eq(reviewSummaryJob), any(JobParameters.class)))
        .thenThrow(new JobInstanceAlreadyCompleteException("이미 완료됨"));

    assertThatCode(
        () -> new ReviewSummaryBatchScheduler(jobOperator, jobRepository, reviewSummaryJob).run()
    ).doesNotThrowAnyException();

    ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
    verify(jobOperator).start(eq(reviewSummaryJob), captor.capture());
    Instant scheduleSlot = Instant.parse(captor.getValue().getString("scheduleSlot"));
    assertThat(scheduleSlot.getNano()).isZero();
    assertThat(scheduleSlot.getEpochSecond() % 60).isZero();
  }
}
