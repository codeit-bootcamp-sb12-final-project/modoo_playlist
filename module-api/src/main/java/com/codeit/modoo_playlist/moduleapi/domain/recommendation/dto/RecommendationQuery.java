package com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecommendationQuery(
    @NotNull
    UUID contentId,
    @Min(1)
    @Max(100)
    Integer limit
) {
  public RecommendationQuery {
    if (limit == null) {
      limit = 10;
    }
  }
}
