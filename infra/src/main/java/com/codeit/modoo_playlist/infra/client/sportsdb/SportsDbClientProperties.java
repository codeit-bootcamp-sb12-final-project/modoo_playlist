package com.codeit.modoo_playlist.infra.client.sportsdb;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties("external.sports-db")
public class SportsDbClientProperties {

    @NotBlank
    private String baseUrl = "https://www.thesportsdb.com/api/v1/json";
    @NotBlank
    private String apiKey;
    @Min(1)
    @Max(29)
    private int requestsPerMinute = 12;
    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(3);
    @NotNull
    private Duration readTimeout = Duration.ofSeconds(10);
    @Min(1)
    private int retryMaxAttempts = 3;
    private Duration retryInitialDelay = Duration.ofSeconds(1);
    private Duration retryMaxDelay = Duration.ofSeconds(4);

    @AssertTrue(message = "TheSportsDB base-url은 https://www.thesportsdb.com 주소여야 합니다.")
    public boolean isBaseUrlTrusted() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return true;
        }
        try {
            URI uri = URI.create(baseUrl);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "www.thesportsdb.com".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @AssertTrue(message = "TheSportsDB connect-timeout은 0초보다 커야 합니다.")
    public boolean isConnectTimeoutPositive() {
        return connectTimeout == null || !connectTimeout.isZero() && !connectTimeout.isNegative();
    }

    @AssertTrue(message = "TheSportsDB read-timeout은 0초보다 커야 합니다.")
    public boolean isReadTimeoutPositive() {
        return readTimeout == null || !readTimeout.isZero() && !readTimeout.isNegative();
    }
}
