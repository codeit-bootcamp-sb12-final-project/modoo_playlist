package com.codeit.modoo_playlist.moduleapi.domain.conversation.repository;

import com.codeit.modoo_playlist.moduleapi.dto.ConversationDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.response.CursorResponseConversationDto;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepositoryCustom{

    // 내가 참여중인 DM 목록
    CursorResponseConversationDto findConversations(
            UUID requesterId,
            SliceCursorRequest request
    );

    // 특정 conversation 조회
    Optional<ConversationDto> findConversation(
            UUID requesterId,
            UUID conversationId
    );

    // 두 사용자 사이의 DM 조회
    Optional<ConversationDto> findDmConversation(
            UUID requesterId,
            UUID otherUserId
    );

    // 특정 사용자가 대화의 참여자인지
    boolean existsParticipant(
            UUID conversationId,
            UUID userId
    );
}
