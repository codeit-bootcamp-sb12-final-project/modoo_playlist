package com.codeit.modoo_playlist.moduleapi.dto.conversation.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConversationCreateRequest (
        @NotNull(message = "상대 사용자 ID는 필수입니다.")
        UUID withUserId
)
{
}
