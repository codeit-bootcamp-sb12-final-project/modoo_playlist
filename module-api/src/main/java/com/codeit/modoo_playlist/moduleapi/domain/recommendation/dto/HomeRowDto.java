package com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto;

import java.util.List;

public record HomeRowDto(
    String title,
    String description,
    List<RecommendedContentDto> contents
) {

}
