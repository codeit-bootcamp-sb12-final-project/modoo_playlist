package com.codeit.modoo_playlist.modulebatch.embedding.config;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingResult;
import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;
import com.codeit.modoo_playlist.modulebatch.embedding.processor.ContentEmbeddingProcessor;
import com.codeit.modoo_playlist.modulebatch.embedding.reader.ContentEmbeddingReader;
import com.codeit.modoo_playlist.modulebatch.embedding.writer.ContentEmbeddingWriter;
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
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class EmbeddingBatchJobConfig {

  private static final int CHUNK_SIZE = 20;

  @Bean
  Job contentEmbeddingJob(
      JobRepository jobRepository,
      Step contentEmbeddingStep
  ) {
    return new JobBuilder("contentEmbeddingJob", jobRepository)
        .start(contentEmbeddingStep)
        .build();
  }

  @Bean
  @StepScope
  ContentEmbeddingReader contentEmbeddingReader(ContentEmbeddingMapper mapper) {
    return new ContentEmbeddingReader(mapper);
  }

  @Bean
  Step contentEmbeddingStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      ContentEmbeddingReader contentEmbeddingReader,
      ContentEmbeddingMapper mapper,
      EmbeddingModel embeddingModel,
      @Value("${spring.ai.google.genai.embedding.text.model}") String configuredModel
  ) {
    return new StepBuilder("contentEmbeddingStep", jobRepository)
        .<ContentEmbeddingTarget, ContentEmbeddingResult>chunk(CHUNK_SIZE)
        .transactionManager(transactionManager)
        .reader(contentEmbeddingReader)
        .processor(new ContentEmbeddingProcessor(embeddingModel, configuredModel))
        .writer(new ContentEmbeddingWriter(mapper))
        .build();
  }
}
