package com.codeit.modoo_playlist.moduleapi.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageDto (
        UUID id,
        UUID conversationId,
        Instant createdAt,
        UserSummary sender,
        UserSummary receiver,
        String content
){
}
