package com.codeit.modoo_playlist.core.domain.message.entity;

import com.codeit.modoo_playlist.core.domain.user.dto.UserSummaryResponse;

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