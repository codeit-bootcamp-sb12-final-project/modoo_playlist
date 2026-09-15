package com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.dto;

import java.util.UUID;

public record SearchContentDto(
    UUID contentId,
    String title,
    String thumbnailUrl,
    double score
) {

}
