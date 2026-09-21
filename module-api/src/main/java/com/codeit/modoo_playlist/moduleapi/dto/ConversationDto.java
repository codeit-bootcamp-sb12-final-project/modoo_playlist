package com.codeit.modoo_playlist.moduleapi.dto;

import com.codeit.modoo_playlist.core.domain.message.entity.MessageDto;
import com.codeit.modoo_playlist.core.domain.user.dto.UserSummaryResponse;

import java.util.UUID;

public record ConversationDto (
        UUID id,
        UserSummaryResponse with,
        MessageDto lastestMessage,
        boolean hasUnread
){
}
