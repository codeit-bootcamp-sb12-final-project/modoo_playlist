package com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response;

import java.util.UUID;

public record ChatDoneEvent(
        UUID conversationId
) {
}
