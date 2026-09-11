package com.codeit.modoo_playlist.modulebatch.tmdb.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties("batch.tmdb")
@Validated
public class TmdbBatchProperties {

    private boolean enabled = true;
    @NotBlank
    private String cron = "0 0 0,12 * * *";
    @NotBlank
    private String zone = "Asia/Seoul";
    @Min(1)
    private int activeMaxPages = 5;
    @Min(1)
    private int popularMaxPages = 2;
    @Min(1)
    private int chunkSize = 10;
    @Min(0)
    private int skipLimit = 20;
    private Set<Integer> excludedTvGenreIds = Set.of(10763, 10764, 10767);
}
