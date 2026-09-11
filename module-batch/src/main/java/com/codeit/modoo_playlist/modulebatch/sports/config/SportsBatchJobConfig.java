package com.codeit.modoo_playlist.modulebatch.sports.config;

import java.util.Set;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
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
import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEvent;
import com.codeit.modoo_playlist.modulebatch.support.ErrorCodeSkipPolicy;
import com.codeit.modoo_playlist.modulebatch.sports.listener.SportsSkipListener;
import com.codeit.modoo_playlist.modulebatch.sports.model.SportsSyncContent;
import com.codeit.modoo_playlist.modulebatch.sports.persistence.SportsContentMapper;
import com.codeit.modoo_playlist.modulebatch.sports.processor.SportsSyncConverter;
import com.codeit.modoo_playlist.modulebatch.sports.processor.SportsSyncItemProcessor;
import com.codeit.modoo_playlist.modulebatch.sports.reader.SportsEventLoader;
import com.codeit.modoo_playlist.modulebatch.sports.reader.SportsPrefetchingReader;
import com.codeit.modoo_playlist.modulebatch.sports.writer.SportsContentWriter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SportsBatchProperties.class)
public class SportsBatchJobConfig {

    @Bean
    Job sportsSyncJob(
            JobRepository jobRepository,
            @Qualifier("sportsSyncStep") Step sportsSyncStep
    ) {
        return new JobBuilder("sportsSyncJob", jobRepository)
                .start(sportsSyncStep)
                .build();
    }

    @Bean
    Step sportsSyncStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("sportsEventReader") SportsPrefetchingReader reader,
            @Qualifier("sportsEventProcessor") ItemProcessor<SportsDbEvent, SportsSyncContent> processor,
            SportsContentWriter writer,
            SportsSkipListener skipListener,
            SportsBatchProperties properties
    ) {
        return new StepBuilder("sportsSyncStep", jobRepository)
                .<SportsDbEvent, SportsSyncContent>chunk(properties.getChunkSize())
                .transactionManager(transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(reader)
                .faultTolerant()
                .skipPolicy(new ErrorCodeSkipPolicy(
                        Set.of(ErrorCode.SPORTSDB_EVENT_INVALID),
                        properties.getSkipLimit()
                ))
                .skipListener(skipListener)
                .build();
    }

    @Bean
    @StepScope
    SportsPrefetchingReader sportsEventReader(SportsEventLoader loader) {
        return new SportsPrefetchingReader(loader);
    }

    @Bean
    @StepScope
    SportsSyncItemProcessor sportsEventProcessor(
            SportsContentMapper contentMapper,
            SportsSyncConverter converter
    ) {
        return new SportsSyncItemProcessor(contentMapper, converter);
    }
}
