package com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto;

import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.content.type.SportsStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ContentDetailDto(
    UUID contentId,
    String title,
    ContentType type,
    Integer releaseYear,
    String originCountry,
    List<String> tags,
    List<String> directors,
    List<String> actors,
    String description,
    SportsInfo sports
) {

  public static ContentDetailDto titleOnly(UUID contentId, String title) {
    return new ContentDetailDto(contentId, title, null, null, null, List.of(), List.of(), List.of(), null, null);
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public record SportsInfo(
      String sportType,
      String league,
      String season,
      String homeTeam,
      String awayTeam,
      String venue,
      SportsStatus status,
      Instant kickoffAt
  ) {

  }
}
