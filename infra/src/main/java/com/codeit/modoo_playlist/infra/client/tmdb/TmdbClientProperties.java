package com.codeit.modoo_playlist.infra.client.tmdb;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties("external.tmdb")
@Validated
public class TmdbClientProperties {

    @NotBlank
    private String baseUrl = "https://api.themoviedb.org/3";
    @NotBlank
    private String imageBaseUrl = "https://image.tmdb.org/t/p/w500";
    @NotBlank
    private String accessToken;
    @NotBlank
    private String language = "ko-KR";
    @NotBlank
    private String region = "KR";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(10);
    @Min(1)
    private int retryMaxAttempts = 3;
    private Duration retryInitialDelay = Duration.ofSeconds(1);
    private Duration retryMaxDelay = Duration.ofSeconds(4);
}
