package com.codeit.modoo_playlist.moduleapi.domain.conversation.service;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationParticipant;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationType;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    private final ConversationMapper conversationMapper;

    // 대화 생성
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ConversationDto create(
            UUID requesterId,
            ConversationCreateRequest request
    ) {
        if (request == null || request.withUserId() == null) {
            throw new BaseException(ErrorCode.REQUIRED_WITH_USER);
        }

        UUID withUserId = request.withUserId();

        // 자기 자신과의 DM 생성 방지
        if (requesterId.equals(withUserId)) {
            throw new BaseException(ErrorCode.SELF_CONVERSATION_NOT_ALLOWED);
        }

        // TODO: 한번에 두 사람을 조회... 조회 횟수를 줄이는 것이 좋아보임.
        // 요청 방향과 관계없이 항상 같은 UUID 순서로 잠금 획득
        boolean requesterFirst = requesterId.compareTo(withUserId) < 0;

        UUID firstUserId = requesterFirst ? requesterId : withUserId;
        UUID secondUserId = requesterFirst ? withUserId : requesterId;

        User firstUser = userRepository.findByIdForUpdate(firstUserId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.USER_NOT_FOUND)
                );

        User secondUser = userRepository.findByIdForUpdate(secondUserId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.USER_NOT_FOUND)
                );

        // 잠금 순서와 별개로 실제 요청자·상대방을 구분
        User requester = requesterFirst ? firstUser : secondUser;
        User withUser = requesterFirst ? secondUser : firstUser;

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
            String keywordLike,
            SliceCursorRequest request
    ) {
        if (!userRepository.existsById(requesterId)) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }

        return conversationRepository.findConversations(requesterId, keywordLike, request);
    }

    // 특정 대화 조회
    @Transactional(readOnly = true)
    public ConversationDto findConversation(
            UUID requesterId,
            UUID conversationId) {

        return conversationRepository
                .findConversation(requesterId, conversationId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.CONVERSATION_NOT_FOUND)
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
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }

        return conversationRepository
                .findDmConversation(requesterId, userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.CONVERSATION_NOT_FOUND)
                );
    }

    // 특정 대화방의 DM 목록 조회
    @Transactional(readOnly = true)
    public CursorResponseMessageDto findMessages(
            UUID requesterId,
            UUID conversationId,
            SliceCursorRequest request
    ) {
        // 대화가 존재하는지 확인
        if (!conversationRepository.existsById(conversationId)) {
            throw new BaseException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        // 요청자가 대화의 참여자인지 확인
        boolean participant = conversationRepository
                .existsParticipant(conversationId, requesterId);

        if (!participant) {
            throw new BaseException(ErrorCode.CONVERSATION_ACCESS_DENIED);
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
                        new BaseException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.isRead()) {
            message.markAsRead();
        }
    }
}
