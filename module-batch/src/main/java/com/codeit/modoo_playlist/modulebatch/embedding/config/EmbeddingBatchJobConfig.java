package com.codeit.modoo_playlist.modulebatch.embedding.config;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingResult;
import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;
import com.codeit.modoo_playlist.modulebatch.embedding.processor.ContentEmbeddingProcessor;
import com.codeit.modoo_playlist.modulebatch.embedding.reader.ContentEmbeddingReader;
import com.codeit.modoo_playlist.modulebatch.embedding.tasklet.ContentEmbeddingCleanupTasklet;
import com.codeit.modoo_playlist.modulebatch.embedding.writer.ContentEmbeddingWriter;
import com.codeit.modoo_playlist.modulebatch.monitoring.BatchMetricsListener;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class EmbeddingBatchJobConfig {

  private static final int CHUNK_SIZE = 50;

  @Bean
  Job contentEmbeddingJob(
      JobRepository jobRepository,
      Step contentEmbeddingStep,
      Step contentEmbeddingCleanupStep,
      BatchMetricsListener metricsListener
  ) {
    return new JobBuilder("contentEmbeddingJob", jobRepository)
        .listener(metricsListener)
        .start(contentEmbeddingStep)
        .next(contentEmbeddingCleanupStep)
        .build();
  }

  @Bean
  @StepScope
  ContentEmbeddingReader contentEmbeddingReader(
      ContentEmbeddingMapper mapper,
      @Value("${batch.embedding.max-items-per-run:100000}") int maxItemsPerRun
  ) {
    return new ContentEmbeddingReader(mapper, maxItemsPerRun);
  }

  @Bean
  Step contentEmbeddingStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ContentEmbeddingReader contentEmbeddingReader,
      ContentEmbeddingMapper mapper,
      ElasticsearchOperations elasticsearchOperations,
      EmbeddingModel embeddingModel,
      @Value("${spring.ai.google.genai.embedding.text.model}") String configuredModel
  ) {
    return new StepBuilder("contentEmbeddingStep", jobRepository)
        .<ContentEmbeddingTarget, ContentEmbeddingResult>chunk(CHUNK_SIZE)
        .transactionManager(transactionManager)
        .reader(contentEmbeddingReader)
        .processor(new ContentEmbeddingProcessor(configuredModel))
        .writer(new ContentEmbeddingWriter(mapper, elasticsearchOperations, embeddingModel))
        .build();
  }

  @Bean
  Step contentEmbeddingCleanupStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ContentEmbeddingMapper mapper,
      ElasticsearchOperations elasticsearchOperations
  ) {
    return new StepBuilder("contentEmbeddingCleanupStep", jobRepository)
        .tasklet(new ContentEmbeddingCleanupTasklet(mapper, elasticsearchOperations), transactionManager)
        .build();
  }
}
