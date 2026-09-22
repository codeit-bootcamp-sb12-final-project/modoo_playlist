package com.codeit.modoo_playlist.modulerealtime.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record DirectMessageSendRequest(
        @NotBlank
        String content
) {
}
