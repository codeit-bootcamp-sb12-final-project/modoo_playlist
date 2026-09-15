package com.codeit.modoo_playlist.moduleapi.dto.content.request;

import java.util.List;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

public record ContentCreateRequest(
        @NotBlank
        @Pattern(
                regexp = "movie|tvSeries|sport",
                message = "type은 movie, tvSeries, sport 중 하나여야 합니다."
        )
        String type,

        @NotBlank
        @Size(max = 255)
        String title,

        String description,

        LocalDate releaseDate,

        @Size(max = 20)
        String originCountry,

        List<@NotBlank @Size(max = 50) String> tags,

        @Valid ContentVideoRequest video,

        @Valid ContentSportsRequest sports,

        List<@NotNull @Valid ContentPersonRequest> people
) {
    public ContentCreateRequest(String type, String title, String description, List<String> tags) {
        this(type, title, description, null, null, tags, null, null, null);
    }
}
