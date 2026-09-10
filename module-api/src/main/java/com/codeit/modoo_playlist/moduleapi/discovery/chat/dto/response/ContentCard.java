package com.codeit.modoo_playlist.moduleapi.discovery.chat.dto.response;

import java.util.UUID;

public record ContentCard(
        UUID contentId,
        String title,
        String posterUrl
) {
}
