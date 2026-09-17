package com.codeit.modoo_playlist.moduleapi.domain.message.service;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationParticipant;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.message.entity.MessageType;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.conversation.repository.ConversationRepository;
import com.codeit.modoo_playlist.moduleapi.domain.message.repository.MessageRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.dto.MessageDto;
import com.codeit.modoo_playlist.moduleapi.dto.chat.ContentChatDto;
import com.codeit.modoo_playlist.moduleapi.dto.chat.ContentChatSendRequest;
import com.codeit.modoo_playlist.moduleapi.dto.chat.DirectMessageSendRequest;
import com.codeit.modoo_playlist.moduleapi.mapper.MessageMapper;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
//import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
//    private final ApplicationEventPublisher eventPublisher;
    private final ContentRepository contentRepository;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    // 1) DM 전송 (+ 메시지 생성)
    @Transactional
    public MessageDto sendDirectMessage(
            UUID senderId,
            UUID conversationId,
            DirectMessageSendRequest payload
    ) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(()-> new BaseException(ErrorCode.USER_NOT_FOUND));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(()-> new BaseException(ErrorCode.CONVERSATION_NOT_FOUND));

        // sender가 해당 대화에 속하는지 검사
        boolean senderParticipates =
                conversation.getParticipants().stream()
                        .anyMatch(participant ->
                                participant.getUser()
                                        .getId()
                                        .equals(senderId));

        if (!senderParticipates) {
            throw new BaseException(
                    ErrorCode.CONVERSATION_ACCESS_DENIED);
        }

        // sender가 아닌 참여자를 receiver로 사용
        User receiver = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(senderId))
                .findFirst()
                .orElseThrow(() ->
                        new BaseException(ErrorCode.CONVERSATION_ACCESS_DENIED));

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(null)
                .conversation(conversation)
                .type(MessageType.DM)
                .message(payload.content())
                .build();

        Message saved = messageRepository.save(message);

        MessageDto response = messageMapper.toDto(saved);
//        eventPublisher.publishEvent(new DMCreatedEvent(receiverUserId, response));
        return response;
    }

    // 2) 실시간 채팅 전송 (+ 메시지 생성)
    @Transactional
    public ContentChatDto sendContentChat(
            UUID senderId,
            UUID contentId,
            ContentChatSendRequest payload
    ) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(()-> new BaseException(ErrorCode.USER_NOT_FOUND));

        Content content = contentRepository.findById(contentId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.CONTENT_NOT_FOUND));

        Message message = Message.builder()
                .sender(sender)
                .receiver(null)
                .content(content)
                .conversation(null)
                .type(MessageType.CONTENT)
                .message(payload.content())
                .build();

        Message saved = messageRepository.save(message);
        ContentChatDto response = new ContentChatDto(
                userMapper.toSummary(sender),
                saved.getMessage()
        );
//        eventPublisher.publishEvent(new DMCreatedEvent(receiverUserId, response));
        return response;
    }
}
