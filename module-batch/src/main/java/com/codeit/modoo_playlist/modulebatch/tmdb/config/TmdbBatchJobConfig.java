package com.codeit.modoo_playlist.modulebatch.tmdb.config;

import java.util.Set;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.modulebatch.support.ErrorCodeSkipPolicy;
import com.codeit.modoo_playlist.modulebatch.tmdb.listener.TmdbSkipListener;
import com.codeit.modoo_playlist.modulebatch.tmdb.listener.TmdbStepFailureListener;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbCandidate.MediaType;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbFetchedContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.persistence.TmdbContentMapper;
import com.codeit.modoo_playlist.modulebatch.tmdb.processor.TmdbSyncConverter;
import com.codeit.modoo_playlist.modulebatch.tmdb.processor.TmdbSyncItemProcessor;
import com.codeit.modoo_playlist.modulebatch.tmdb.processor.TmdbSyncItemProcessor.SyncMode;
import com.codeit.modoo_playlist.modulebatch.tmdb.reader.TmdbCandidateLoader;
import com.codeit.modoo_playlist.modulebatch.tmdb.reader.TmdbPrefetchingReader;
import com.codeit.modoo_playlist.modulebatch.tmdb.writer.TmdbContentWriter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TmdbBatchProperties.class)
public class TmdbBatchJobConfig {

    private static final String FATAL_EXIT = "TMDB_FATAL";

    @Bean
    Job tmdbSyncJob(
            JobRepository jobRepository,
            @Qualifier("tmdbMovieActiveStep") Step movieActiveStep,
            @Qualifier("tmdbMoviePopularStep") Step moviePopularStep,
            @Qualifier("tmdbTvActiveStep") Step tvActiveStep,
            @Qualifier("tmdbTvPopularStep") Step tvPopularStep,
            @Qualifier("tmdbCompletionDecider") JobExecutionDecider completionDecider
    ) {
        return new JobBuilder("tmdbSyncJob", jobRepository)
                .start(movieActiveStep).on(FATAL_EXIT).fail()
                .from(movieActiveStep).on("*").to(moviePopularStep)
                .from(moviePopularStep).on(FATAL_EXIT).fail()
                .from(moviePopularStep).on("*").to(tvActiveStep)
                .from(tvActiveStep).on(FATAL_EXIT).fail()
                .from(tvActiveStep).on("*").to(tvPopularStep)
                .from(tvPopularStep).on(FATAL_EXIT).fail()
                .from(tvPopularStep).on("*").to(completionDecider)
                .from(completionDecider).on(FlowExecutionStatus.FAILED.getName()).fail()
                .from(completionDecider).on("*").end()
                .build()
                .build();
    }

    @Bean
    JobExecutionDecider tmdbCompletionDecider() {
        return (jobExecution, stepExecution) -> jobExecution.getStepExecutions().stream()
                .anyMatch(execution -> execution.getStatus() == BatchStatus.FAILED)
                ? FlowExecutionStatus.FAILED
                : FlowExecutionStatus.COMPLETED;
    }

    @Bean
    Step tmdbMovieActiveStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("tmdbMovieActiveReader") TmdbPrefetchingReader reader,
            @Qualifier("tmdbMovieActiveProcessor") ItemProcessor<TmdbFetchedContent, TmdbSyncContent> processor,
            TmdbContentWriter writer,
            TmdbSkipListener skipListener,
            TmdbStepFailureListener skipFailureListener,
            TmdbBatchProperties properties
    ) {
        return buildStep("tmdbMovieActiveStep", jobRepository, transactionManager, reader, processor,
                writer, skipListener, skipFailureListener, properties);
    }

    @Bean
    Step tmdbMoviePopularStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("tmdbMoviePopularReader") TmdbPrefetchingReader reader,
            @Qualifier("tmdbMoviePopularProcessor") ItemProcessor<TmdbFetchedContent, TmdbSyncContent> processor,
            TmdbContentWriter writer,
            TmdbSkipListener skipListener,
            TmdbStepFailureListener skipFailureListener,
            TmdbBatchProperties properties
    ) {
        return buildStep("tmdbMoviePopularStep", jobRepository, transactionManager, reader, processor,
                writer, skipListener, skipFailureListener, properties);
    }

    @Bean
    Step tmdbTvActiveStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("tmdbTvActiveReader") TmdbPrefetchingReader reader,
            @Qualifier("tmdbTvActiveProcessor") ItemProcessor<TmdbFetchedContent, TmdbSyncContent> processor,
            TmdbContentWriter writer,
            TmdbSkipListener skipListener,
            TmdbStepFailureListener skipFailureListener,
            TmdbBatchProperties properties
    ) {
        return buildStep("tmdbTvActiveStep", jobRepository, transactionManager, reader, processor,
                writer, skipListener, skipFailureListener, properties);
    }

    @Bean
    Step tmdbTvPopularStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("tmdbTvPopularReader") TmdbPrefetchingReader reader,
            @Qualifier("tmdbTvPopularProcessor") ItemProcessor<TmdbFetchedContent, TmdbSyncContent> processor,
            TmdbContentWriter writer,
            TmdbSkipListener skipListener,
            TmdbStepFailureListener skipFailureListener,
            TmdbBatchProperties properties
    ) {
        return buildStep("tmdbTvPopularStep", jobRepository, transactionManager, reader, processor,
                writer, skipListener, skipFailureListener, properties);
    }

    private Step buildStep(
            String name,
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TmdbPrefetchingReader reader,
            ItemProcessor<TmdbFetchedContent, TmdbSyncContent> processor,
            TmdbContentWriter writer,
            TmdbSkipListener skipListener,
            TmdbStepFailureListener stepFailureListener,
            TmdbBatchProperties properties
    ) {
        return new StepBuilder(name, jobRepository)
                .<TmdbFetchedContent, TmdbSyncContent>chunk(properties.getChunkSize())
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(reader)
                .faultTolerant()
                .skipPolicy(new ErrorCodeSkipPolicy(
                        Set.of(ErrorCode.TMDB_CONTENT_INVALID, ErrorCode.TMDB_ITEM_FETCH_FAILED),
                        properties.getSkipLimit()
                ))
                .skipListener(skipListener)
                .listener(stepFailureListener)
                .build();
    }

    @Bean
    @StepScope
    TmdbPrefetchingReader tmdbMovieActiveReader(TmdbCandidateLoader loader) {
        return new TmdbPrefetchingReader(loader::loadMovieActive);
    }

    @Bean
    @StepScope
    TmdbPrefetchingReader tmdbMoviePopularReader(TmdbCandidateLoader loader) {
        return new TmdbPrefetchingReader(loader::loadMoviePopular);
    }

    @Bean
    @StepScope
    TmdbPrefetchingReader tmdbTvActiveReader(TmdbCandidateLoader loader) {
        return new TmdbPrefetchingReader(loader::loadTvActive);
    }

    @Bean
    @StepScope
    TmdbPrefetchingReader tmdbTvPopularReader(TmdbCandidateLoader loader) {
        return new TmdbPrefetchingReader(loader::loadTvPopular);
    }

    @Bean
    @StepScope
    TmdbSyncItemProcessor tmdbMovieActiveProcessor(
            TmdbContentMapper mapper,
            TmdbSyncConverter converter
    ) {
        return new TmdbSyncItemProcessor(MediaType.MOVIE, SyncMode.ACTIVE, mapper, converter);
    }

    @Bean
    @StepScope
    TmdbSyncItemProcessor tmdbMoviePopularProcessor(
            TmdbContentMapper mapper,
            TmdbSyncConverter converter
    ) {
        return new TmdbSyncItemProcessor(MediaType.MOVIE, SyncMode.POPULAR, mapper, converter);
    }

    @Bean
    @StepScope
    TmdbSyncItemProcessor tmdbTvActiveProcessor(
            TmdbContentMapper mapper,
            TmdbSyncConverter converter
    ) {
        return new TmdbSyncItemProcessor(MediaType.TV, SyncMode.ACTIVE, mapper, converter);
    }

    @Bean
    @StepScope
    TmdbSyncItemProcessor tmdbTvPopularProcessor(
            TmdbContentMapper mapper,
            TmdbSyncConverter converter
    ) {
        return new TmdbSyncItemProcessor(MediaType.TV, SyncMode.POPULAR, mapper, converter);
    }
}
