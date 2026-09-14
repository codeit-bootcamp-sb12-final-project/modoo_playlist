package com.codeit.modoo_playlist.moduleapi.dto.chat.tool;

import java.util.UUID;

public record SearchContentDto(
    UUID contentId,
    String title,
    String thumbnailUrl,
    double score
) {

}
