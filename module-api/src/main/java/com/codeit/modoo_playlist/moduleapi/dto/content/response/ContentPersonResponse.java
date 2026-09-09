package com.codeit.modoo_playlist.moduleapi.dto.content.response;

import java.util.UUID;

public record ContentPersonResponse(
        UUID id,
        String roleType,
        String personName,
        String characterName,
        int displayOrder,
        String personId,
        String personImg
) {
}
