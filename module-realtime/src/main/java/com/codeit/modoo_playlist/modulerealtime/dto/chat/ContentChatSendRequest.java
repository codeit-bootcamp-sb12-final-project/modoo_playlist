package com.codeit.modoo_playlist.modulerealtime.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ContentChatSendRequest (
        @NotBlank String content
){
}
