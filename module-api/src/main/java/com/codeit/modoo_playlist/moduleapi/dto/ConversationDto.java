package com.codeit.modoo_playlist.moduleapi.dto;

import java.util.UUID;

public record ConversationDto (
        UUID id,
        UserDto with,
        MessageDto latestMessage,
        boolean hasUnread
){
}
