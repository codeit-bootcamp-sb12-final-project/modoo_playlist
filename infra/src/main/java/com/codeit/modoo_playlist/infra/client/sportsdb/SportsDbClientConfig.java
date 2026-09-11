package com.codeit.modoo_playlist.infra.client.sportsdb;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SportsDbClientProperties.class)
public class SportsDbClientConfig {

    @Bean
    RestClient sportsDbRestClient(RestClient.Builder builder, SportsDbClientProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    RetryTemplate sportsDbRetryTemplate(SportsDbClientProperties properties) {
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
    SportsDbRateLimiter sportsDbRateLimiter(SportsDbClientProperties properties) {
        return new SportsDbRateLimiter(properties.getRequestsPerMinute());
    }

    @Bean
    SportsDbClient sportsDbClient(
            @Qualifier("sportsDbRestClient") RestClient sportsDbRestClient,
            SportsDbClientProperties properties,
            @Qualifier("sportsDbRetryTemplate") RetryTemplate sportsDbRetryTemplate,
            SportsDbRateLimiter sportsDbRateLimiter
    ) {
        return new SportsDbClient(
                sportsDbRestClient,
                properties,
                sportsDbRetryTemplate,
                sportsDbRateLimiter
        );
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
