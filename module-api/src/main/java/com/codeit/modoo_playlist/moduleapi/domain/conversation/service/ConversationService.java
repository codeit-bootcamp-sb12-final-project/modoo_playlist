package com.codeit.modoo_playlist.moduleapi.domain.conversation.service;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationParticipant;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationType;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.domain.conversation.repository.ConversationRepository;
import com.codeit.modoo_playlist.moduleapi.domain.message.repository.MessageRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.dto.ConversationDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.ConversationCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.response.CursorResponseConversationDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.response.CursorResponseMessageDto;
import com.codeit.modoo_playlist.moduleapi.mapper.ConversationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    private final ConversationMapper conversationMapper;

    // 대화 생성
    @Transactional
    public ConversationDto create(
            UUID requesterId,
            ConversationCreateRequest request
    ) {
        if (request == null || request.withUserId() == null) {
            throw new IllegalArgumentException("상대 사용자 ID는 필수입니다.");
        }

        UUID withUserId = request.withUserId();

        // 자기 자신과의 DM 생성 방지
        if (requesterId.equals(withUserId)) {
            throw new IllegalArgumentException("대화방은 발신자와 수신자가 같을 수 없습니다.");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        User withUser = userRepository.findById(withUserId)
                .orElseThrow(() -> new IllegalArgumentException("상대 사용자를 찾을 수 없습니다."));


        return conversationRepository
                .findDmConversation(requesterId, withUserId)
                .orElseGet(() -> {

                    Conversation conversation = Conversation
                            .builder()
                            .type(ConversationType.DM)
                            .build();

                    //사용자 저장
                    ConversationParticipant.create(conversation, requester);
                    ConversationParticipant.create(conversation, withUser);

                    Conversation saved = conversationRepository.save(conversation);

                    return conversationMapper.toDto(
                            saved,
                            withUser,
                            null,
                            false
                    );
                });
    }


    // 대화 조회 (페이지네이션 + 본인이 포함된 conversation만 조회)
    @Transactional(readOnly = true)
    public CursorResponseConversationDto findConversations(
            UUID requesterId,
            SliceCursorRequest request
    ) {
        if (!userRepository.existsById(requesterId)) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        return conversationRepository.findConversations(requesterId, request);
    }

    // 특정 대화 조회
    @Transactional(readOnly = true)
    public ConversationDto findConversation(
            UUID requesterId,
            UUID conversationId) {

        return conversationRepository
                .findConversation(requesterId, conversationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("대화를 찾을 수 없습니다.")
                );
    }

    // 특정 사용자와의 DM 조회
    @Transactional(readOnly = true)
    public ConversationDto findByReceiverId(UUID requesterId, UUID userId) {

        // 본인과의 대화 조회 방지
        if (requesterId.equals(userId)) {
            throw new IllegalArgumentException("자기 자신과의 대화는 조회할 수 없습니다.");
        }

        //상대방 사용자 존재 여부
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("상대 사용자를 찾을 수 없습니다.");
        }

        return conversationRepository
                .findDmConversation(requesterId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("해당 사용자와의 대화를 찾을 수 없습니다.")
                );
    }

    // 특정 대화방의 DM 목록 조회
    @Transactional(readOnly = true)
    public CursorResponseMessageDto findMessages(
            UUID requesterId,
            UUID conversationId,
            SliceCursorRequest request
    ) {
        boolean participant = conversationRepository
                .existsParticipant(conversationId, requesterId);

        if (!participant) {
            throw new IllegalArgumentException("해당 대화에 참여하고 있지 않습니다.");
        }

        return messageRepository.findMessages(conversationId, request);
    }

    // DM 읽음 처리
    @Transactional
    public void readDirectMessage(
            UUID requesterId,
            UUID conversationId,
            UUID messageId
    ) {
        Message message = messageRepository
                .findMessageForRead(
                        messageId,
                        conversationId,
                        requesterId
                ).orElseThrow(() ->
                        new IllegalArgumentException("읽음 처리할 메시지를 찾을 수 없습니다.")
                );

        if (!message.isRead()) {
            message.markAsRead();
        }
    }
}
