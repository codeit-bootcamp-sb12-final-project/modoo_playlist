package com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto;

import java.util.List;

public record HomeFeedResponse(
    List<HomeRowDto> rows
) {

}
