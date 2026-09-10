package com.codeit.modoo_playlist.infra.client.tmdb;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TmdbClientProperties.class)
public class TmdbClientConfig {

    @Bean
    RestClient tmdbRestClient(RestClient.Builder builder, TmdbClientProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    RetryTemplate tmdbRetryTemplate(TmdbClientProperties properties) {
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxRetries(properties.getRetryMaxAttempts() - 1L)
                .delay(properties.getRetryInitialDelay())
                .multiplier(2.0)
                .maxDelay(properties.getRetryMaxDelay())
                .predicate(this::isRetryable)
                .build();
        return new RetryTemplate(retryPolicy);
    }

    @Bean
    TmdbClient tmdbClient(
            RestClient tmdbRestClient,
            TmdbClientProperties properties,
            RetryTemplate tmdbRetryTemplate
    ) {
        return new TmdbClient(tmdbRestClient, properties, tmdbRetryTemplate);
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof ResourceAccessException) {
            return true;
        }
        if (throwable instanceof RestClientResponseException exception) {
            return exception.getStatusCode().value() == 429 || exception.getStatusCode().is5xxServerError();
        }
        return throwable instanceof RestClientException;
    }
}
