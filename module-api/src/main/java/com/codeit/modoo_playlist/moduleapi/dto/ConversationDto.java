package com.codeit.modoo_playlist.moduleapi.dto;

import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.user.entity.User;

import java.util.UUID;

public record ConversationDto (
        UUID id,
        User with,
        Message latestMessage,
        boolean hasUnread
){
}
