package com.codeit.modoo_playlist.modulebatch.reviewsummary.config;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.modulebatch.monitoring.BatchMetricsListener;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.listener.ReviewSummarySkipListener;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryResult;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.persistence.ReviewSummaryMapper;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.processor.ReviewSummaryProcessor;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.reader.ReviewSummaryReader;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.writer.ReviewSummaryWriter;
import com.codeit.modoo_playlist.modulebatch.support.ErrorCodeSkipPolicy;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class ReviewSummaryBatchJobConfig {

  private static final int CHUNK_SIZE = 20;

  @Bean
  Job reviewSummaryJob(
      JobRepository jobRepository,
      Step reviewSummaryStep,
      BatchMetricsListener metricsListener
  ) {
    return new JobBuilder("reviewSummaryJob", jobRepository)
        .listener(metricsListener)
        .start(reviewSummaryStep)
        .build();
  }

  @Bean
  @StepScope
  ReviewSummaryReader reviewSummaryReader(
      ReviewSummaryMapper mapper,
      @Value("${batch.review-summary.min-reviews:3}") int minReviews,
      @Value("${batch.review-summary.max-reviews-per-content:30}") int maxReviewsPerContent,
      @Value("${batch.review-summary.max-items-per-run:5000}") int maxItemsPerRun
  ) {
    return new ReviewSummaryReader(mapper, minReviews, maxReviewsPerContent, maxItemsPerRun);
  }

  @Bean
  ChatClient reviewSummaryChatClient(ChatClient.Builder chatClientBuilder) {
    return chatClientBuilder.build();
  }

  @Bean
  Step reviewSummaryStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ReviewSummaryReader reviewSummaryReader,
      ReviewSummaryMapper mapper,
      ChatClient reviewSummaryChatClient,
      ReviewSummarySkipListener skipListener,
      @Value("${batch.review-summary.skip-limit:20}") int skipLimit
  ) {
    return new StepBuilder("reviewSummaryStep", jobRepository)
        .<ReviewSummaryTarget, ReviewSummaryResult>chunk(CHUNK_SIZE)
        .transactionManager(transactionManager)
        .reader(reviewSummaryReader)
        .processor(new ReviewSummaryProcessor(reviewSummaryChatClient))
        .writer(new ReviewSummaryWriter(mapper))
        .faultTolerant()
        .skipPolicy(new ErrorCodeSkipPolicy(
            Set.of(ErrorCode.REVIEW_SUMMARY_GENERATION_FAILED), skipLimit))
        .skipListener(skipListener)
        .build();
  }
}
