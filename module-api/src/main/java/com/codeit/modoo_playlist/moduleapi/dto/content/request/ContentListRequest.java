package com.codeit.modoo_playlist.moduleapi.dto.content.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContentListRequest(
        @Pattern(
                regexp = "movie|tvSeries|sport",
                message = "typeEqual은 movie, tvSeries, sport 중 하나여야 합니다."
        )
        String typeEqual,

        @Size(max = 255)
        String keywordLike,

        List<@NotBlank @Size(max = 50) String> tagsIn,

        String cursor,

        UUID idAfter,

        @Min(1)
        @Max(100)
        Integer limit,

        @Pattern(
                regexp = "ASCENDING|DESCENDING",
                message = "sortDirection은 ASCENDING 또는 DESCENDING이어야 합니다."
        )
        String sortDirection,

        @Pattern(
                regexp = "recommended|watcherCount|createdAt|rate|averageRating",
                message = "지원하지 않는 sortBy 값입니다."
        )
        String sortBy
) {
}
