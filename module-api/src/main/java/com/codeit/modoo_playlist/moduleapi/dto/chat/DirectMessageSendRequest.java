package com.codeit.modoo_playlist.moduleapi.dto.chat;

import jakarta.validation.constraints.NotNull;

public record DirectMessageSendRequest(
        @NotNull
        String content
) {
}
