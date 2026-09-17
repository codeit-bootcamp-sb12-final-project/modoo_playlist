package com.codeit.modoo_playlist.core.global.common.dto;

import com.codeit.modoo_playlist.core.domain.message.entity.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID senderId,
        UUID receiverId,
        UUID contentId,
        UUID conversationId,
        MessageType messageType,
        String message,
        boolean isRead,
        Instant createdAt
) {
}
