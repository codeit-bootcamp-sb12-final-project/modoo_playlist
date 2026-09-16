package com.codeit.modoo_playlist.moduleapi.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ContentChatSendRequest (
        @NotBlank String content
){
}
