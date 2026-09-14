package com.codeit.modoo_playlist.modulebatch.sports.config;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties("batch.sports")
public class SportsBatchProperties {

    private boolean enabled = true;
    @NotBlank
    private String cron = "0 0 6,18 * * *";
    @NotBlank
    private String zone = "Asia/Seoul";
    @Min(1)
    @Max(7)
    private int discoveryDays = 3;
    @Min(1)
    private int chunkSize = 10;
    @Min(0)
    private int skipLimit = 20;
    private boolean statusUpdateEnabled = true;
    @NotBlank
    private String statusCron = "0 */15 * * * *";
    @Min(1)
    private int soccerDurationMinutes = 150;
    @Min(1)
    private int basketballDurationMinutes = 180;
    @Min(1)
    private int baseballDurationMinutes = 240;
    @Min(1)
    private int defaultDurationMinutes = 180;
    @NotEmpty
    private List<@Valid League> leagues = List.of(
            new League("4328", "English Premier League"),
            new League("4335", "Spanish La Liga"),
            new League("4332", "Italian Serie A"),
            new League("4331", "German Bundesliga"),
            new League("4480", "UEFA Champions League"),
            new League("4387", "NBA"),
            new League("4424", "MLB"),
            new League("4830", "Korean KBO League")
    );

    public record League(@NotBlank String id, @NotBlank String name) {
    }

    public int durationMinutes(String sportType) {
        if (sportType == null) {
            return defaultDurationMinutes;
        }
        String normalized = sportType.toLowerCase(Locale.ROOT);
        if (normalized.contains("soccer") || normalized.contains("football")) {
            return soccerDurationMinutes;
        }
        if (normalized.contains("basketball")) {
            return basketballDurationMinutes;
        }
        if (normalized.contains("baseball")) {
            return baseballDurationMinutes;
        }
        return defaultDurationMinutes;
    }
}
