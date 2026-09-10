package com.codeit.modoo_playlist.moduleapi.domain.message.repository;

import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.response.CursorResponseMessageDto;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepositoryCustom {
    CursorResponseMessageDto findMessages(
            UUID conversationId,
            SliceCursorRequest request
    );

    Optional<Message> findMessageForRead(
            UUID messageId,
            UUID conversationId,
            UUID receiverId
    );
}
