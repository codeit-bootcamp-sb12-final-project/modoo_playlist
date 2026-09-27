package com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ChatConversationSummaryDto(
    UUID id,
    String title,
    Instant createdAt
) {

}
