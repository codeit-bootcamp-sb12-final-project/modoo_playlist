package com.codeit.modoo_playlist.moduleapi.domain.chat.service.impl;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationParticipant;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationType;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.message.entity.MessageType;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ChatDoneEvent;
import com.codeit.modoo_playlist.moduleapi.domain.chat.exception.ChatAccessDeniedException;
import com.codeit.modoo_playlist.moduleapi.domain.chat.exception.ChatNotFoundException;
import com.codeit.modoo_playlist.moduleapi.domain.chat.service.ChatService;
import com.codeit.modoo_playlist.moduleapi.domain.conversation.repository.ConversationRepository;
import com.codeit.modoo_playlist.moduleapi.domain.message.repository.MessageRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

  private static final UUID AI_BOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final ChatClient chatClient;
  private final ConversationRepository conversationRepository;
  private final UserRepository userRepository;
  private final MessageRepository messageRepository;

  @Override
  @Transactional
  public Flux<ServerSentEvent<Object>> chat(UUID userId, UUID conversationId, String message) {
    Conversation conversation;
    User user = userRepository.findById(userId).orElseThrow(IllegalArgumentException::new);
    User bot = userRepository.findById(AI_BOT_ID).orElseThrow(IllegalStateException::new);
    if (conversationId != null) {
        conversation = conversationRepository.findById(conversationId)
            .orElseThrow(ChatNotFoundException::new);
      boolean isParticipant = conversation.getParticipants().stream()
          .anyMatch(p -> p.getUser().getId().equals(userId));
      if (!conversation.getType().equals(ConversationType.AI) || !isParticipant) {
        throw new ChatAccessDeniedException();
      }
    } else {
      Conversation newConversation = Conversation.builder()
          .type(ConversationType.AI)
          .build();
      ConversationParticipant.create(newConversation, user);
      conversation = conversationRepository.save(newConversation);
    }

    messageRepository.save(Message.builder()
        .sender(user)
        .receiver(bot)
        .conversation(conversation)
        .type(MessageType.AI)
        .message(message)
        .build());

    StringBuilder responseBuilder = new StringBuilder();

    Flux<ServerSentEvent<Object>> messageEvents = chatClient.prompt()
        .user(message)
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversation.getId().toString()))
        .stream()
        .content()
        .doOnNext(responseBuilder::append)
        .doOnComplete(() -> messageRepository.save(Message.builder()
            .sender(bot)
            .receiver(user)
            .conversation(conversation)
            .type(MessageType.AI)
            .message(responseBuilder.toString())
            .build())
        )
        .map(token -> ServerSentEvent.builder((Object) token)
            .event("message").build());

    Flux<ServerSentEvent<Object>> doneEvent = Flux.just(
        ServerSentEvent.builder((Object) new ChatDoneEvent(conversation.getId())).event("done")
            .build()
    );

    return messageEvents.concatWith(doneEvent);
  }

  @Override
  public Flux<ServerSentEvent<Object>> chatAnonymous(UUID conversationId, String message) {
    UUID sessionId = (conversationId != null) ? conversationId : UUID.randomUUID();

    Flux<ServerSentEvent<Object>> messageEvents = chatClient.prompt()
        .user(message)
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId.toString()))
        .stream()
        .content()
        .map(token -> ServerSentEvent.builder((Object) token)
            .event("message").build());

    Flux<ServerSentEvent<Object>> doneEvent = Flux.just(
        ServerSentEvent.builder((Object) new ChatDoneEvent(sessionId)).event("done")
            .build()
    );

    return messageEvents.concatWith(doneEvent);
  }
}
