package com.codeit.modoo_playlist.moduleapi.domain.chat.service.impl;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationParticipant;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationType;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.message.entity.MessageType;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ChatCardsEvent;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ChatDoneEvent;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ChatErrorEvent;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
import com.codeit.modoo_playlist.moduleapi.domain.chat.exception.ChatAccessDeniedException;
import com.codeit.modoo_playlist.moduleapi.domain.chat.exception.ChatNotFoundException;
import com.codeit.modoo_playlist.moduleapi.domain.chat.service.ChatService;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.ChatToolContext;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.ContentCardCollector;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.GetContentDetailTool;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.GetPersonalizedRecommendationsTool;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.GetTrendingTool;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.GetUserPreferenceTool;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.RecommendContentsTool;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.SearchContentsTool;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.SummarizeReviewsTool;
import com.codeit.modoo_playlist.moduleapi.domain.conversation.repository.ConversationRepository;
import com.codeit.modoo_playlist.moduleapi.domain.message.repository.MessageRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
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

  private final ChatClient chatClient;
  private final SearchContentsTool searchContentsTool;
  private final RecommendContentsTool recommendContentsTool;
  private final GetUserPreferenceTool getUserPreferenceTool;
  private final GetPersonalizedRecommendationsTool getPersonalizedRecommendationsTool;
  private final GetTrendingTool getTrendingTool;
  private final SummarizeReviewsTool summarizeReviewsTool;
  private final GetContentDetailTool getContentDetailTool;
  private final ConversationRepository conversationRepository;
  private final UserRepository userRepository;
  private final MessageRepository messageRepository;

  @Override
  @Transactional
  public Flux<ServerSentEvent<Object>> chat(UUID userId, UUID conversationId, String message) {
    log.info("chat 요청: userId={}, conversationId={}", userId, conversationId);
    Conversation conversation;
    User user = userRepository.findById(userId).orElseThrow(IllegalArgumentException::new);
    User bot = userRepository.findByRole(UserRole.BOT)
        .orElseThrow(() -> new BaseException(ErrorCode.BOT_SERVICE_UNAVAILABLE));
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
    ContentCardCollector cardCollector = new ContentCardCollector();

    Flux<ServerSentEvent<Object>> messageEvents = chatClient.prompt()
        .user(message)
        .tools(searchContentsTool, recommendContentsTool, getUserPreferenceTool,
            getPersonalizedRecommendationsTool, getTrendingTool, summarizeReviewsTool, getContentDetailTool)
        .toolContext(Map.of(ChatToolContext.USER_ID, userId, ChatToolContext.CARD_COLLECTOR, cardCollector))
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversation.getId().toString()))
        .stream()
        .content()
        .doOnNext(responseBuilder::append)
        .doOnComplete(() -> {
          log.info("chat 완료: conversationId={}", conversation.getId());
          messageRepository.save(Message.builder()
              .sender(bot)
              .receiver(user)
              .conversation(conversation)
              .type(MessageType.AI)
              .message(responseBuilder.toString())
              .build());
        })
        .map(token -> ServerSentEvent.builder((Object) token)
            .event("message").build())
        .onErrorResume(e -> {
          log.error("chat 스트림 오류: conversationId={}", conversation.getId(), e);
          return Flux.just(ServerSentEvent.builder(
                  (Object) new ChatErrorEvent("답변을 생성하지 못했어요. 잠시 후 다시 시도해 주세요."))
              .event("error").build());
        });

    Flux<ServerSentEvent<Object>> cardsEvent = cardsEvent(cardCollector);

    Flux<ServerSentEvent<Object>> doneEvent = Flux.just(
        ServerSentEvent.builder((Object) new ChatDoneEvent(conversation.getId())).event("done")
            .build()
    );

    return messageEvents.concatWith(cardsEvent).concatWith(doneEvent);
  }

  private Flux<ServerSentEvent<Object>> cardsEvent(ContentCardCollector cardCollector) {
    return Flux.defer(() -> {
      List<ContentCardDto> cards = cardCollector.getCards();
      if (cards.isEmpty()) {
        return Flux.empty();
      }
      return Flux.just(ServerSentEvent.builder((Object) new ChatCardsEvent(cards)).event("cards").build());
    });
  }
}
