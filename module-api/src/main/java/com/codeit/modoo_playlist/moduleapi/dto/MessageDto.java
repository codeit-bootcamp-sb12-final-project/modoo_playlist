package com.codeit.modoo_playlist.moduleapi.dto;

import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;
import java.time.Instant;
import java.util.UUID;

public record MessageDto(
    UUID id,
    UUID conversationId,
    Instant createdAt,
    UserSummaryResponse sender,
    UserSummaryResponse receiver,
    String content
) {

}
