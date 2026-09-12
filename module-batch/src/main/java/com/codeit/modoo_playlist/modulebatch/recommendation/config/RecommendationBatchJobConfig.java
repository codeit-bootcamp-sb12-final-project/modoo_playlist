package com.codeit.modoo_playlist.modulebatch.recommendation.config;

import com.codeit.modoo_playlist.modulebatch.recommendation.persistence.RecommendationRecalcMapper;
import com.codeit.modoo_playlist.modulebatch.recommendation.tasklet.PreferenceRecalcTasklet;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class RecommendationBatchJobConfig {

	@Bean
	Job nightlyRecalcJob(
			JobRepository jobRepository,
			Step preferenceRecalcStep
	) {
		return new JobBuilder("nightlyRecalcJob", jobRepository)
				.start(preferenceRecalcStep)
				.build();
	}

	@Bean
	Step preferenceRecalcStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			RecommendationRecalcMapper mapper
	) {
		return new StepBuilder("preferenceRecalcStep", jobRepository)
				.tasklet(new PreferenceRecalcTasklet(mapper), transactionManager)
				.build();
	}
}
