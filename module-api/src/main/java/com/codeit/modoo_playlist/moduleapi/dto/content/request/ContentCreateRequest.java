package com.codeit.modoo_playlist.moduleapi.dto.content.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

        List<@NotBlank @Size(max = 50) String> tags
) {
}
