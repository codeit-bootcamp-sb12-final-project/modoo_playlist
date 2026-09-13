package com.codeit.modoo_playlist.moduleapi.dto;

import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;

import java.util.UUID;

public record ConversationDto (
        UUID id,
        UserSummaryResponse with,
        MessageDto lastestMessage,
        boolean hasUnread
){
}
