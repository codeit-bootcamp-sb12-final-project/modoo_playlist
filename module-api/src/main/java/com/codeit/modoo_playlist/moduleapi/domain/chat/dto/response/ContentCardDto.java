package com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response;

import java.util.UUID;

public record ContentCardDto(
    UUID contentId,
    String title,
    String thumbnailUrl
) {
}
