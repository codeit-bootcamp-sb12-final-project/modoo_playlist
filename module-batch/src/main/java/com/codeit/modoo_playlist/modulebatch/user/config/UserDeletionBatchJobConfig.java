package com.codeit.modoo_playlist.modulebatch.user.config;

import com.codeit.modoo_playlist.modulebatch.monitoring.BatchMetricsListener;
import com.codeit.modoo_playlist.modulebatch.user.model.UserDeletionTarget;
import com.codeit.modoo_playlist.modulebatch.user.persistence.UserDeletionMapper;
import com.codeit.modoo_playlist.modulebatch.user.reader.UserDeletionReader;
import com.codeit.modoo_playlist.modulebatch.user.writer.UserDeletionWriter;
import java.time.Instant;
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
public class UserDeletionBatchJobConfig {

  @Bean
  Job userDeletionJob(
      JobRepository jobRepository,
      Step userDeletionStep,
      BatchMetricsListener metricsListener
  ) {
    return new JobBuilder("userDeletionJob", jobRepository)
        .listener(metricsListener)
        .start(userDeletionStep)
        .build();
  }

  @Bean
  @StepScope
  UserDeletionReader userDeletionReader(
      UserDeletionMapper mapper,
      @Value("#{jobParameters['deletionCutoff']}") String deletionCutoff,
      @Value("${batch.user-deletion.page-size:100}") int pageSize,
      @Value("${batch.user-deletion.max-items-per-run:10000}") int maxItemsPerRun
  ) {
    return new UserDeletionReader(
        mapper,
        Instant.parse(deletionCutoff),
        pageSize,
        maxItemsPerRun
    );
  }

  @Bean
  @StepScope
  UserDeletionWriter userDeletionWriter(
      UserDeletionMapper mapper,
      @Value("#{jobParameters['deletionCutoff']}") String deletionCutoff
  ) {
    return new UserDeletionWriter(mapper, Instant.parse(deletionCutoff));
  }

  @Bean
  Step userDeletionStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      UserDeletionReader userDeletionReader,
      UserDeletionWriter userDeletionWriter,
      @Value("${batch.user-deletion.chunk-size:20}") int chunkSize
  ) {
    return new StepBuilder("userDeletionStep", jobRepository)
        .<UserDeletionTarget, UserDeletionTarget>chunk(chunkSize)
        .transactionManager(transactionManager)
        .reader(userDeletionReader)
        .writer(userDeletionWriter)
        .build();
  }
}
