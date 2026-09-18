package com.codeit.modoo_playlist.moduleapi.domain.chat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationParticipant;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationType;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ChatDoneEvent;
import com.codeit.modoo_playlist.moduleapi.domain.chat.exception.ChatAccessDeniedException;
import com.codeit.modoo_playlist.moduleapi.domain.chat.exception.ChatNotFoundException;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.ChatToolContext;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.ContentCardCollector;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.GetPersonalizedRecommendationsTool;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.GetUserPreferenceTool;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.RecommendContentsTool;
import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.SearchContentsTool;
import com.codeit.modoo_playlist.moduleapi.domain.conversation.repository.ConversationRepository;
import com.codeit.modoo_playlist.moduleapi.domain.message.repository.MessageRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;

import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

  @Mock private ChatClient chatClient;
  @Mock private ChatClient.ChatClientRequestSpec requestSpec;
  @Mock private ChatClient.StreamResponseSpec streamResponseSpec;
  @Mock private SearchContentsTool searchContentsTool;
  @Mock private RecommendContentsTool recommendContentsTool;
  @Mock private GetUserPreferenceTool getUserPreferenceTool;
  @Mock private GetPersonalizedRecommendationsTool getPersonalizedRecommendationsTool;
  @Mock private ConversationRepository conversationRepository;
  @Mock private UserRepository userRepository;
  @Mock private MessageRepository messageRepository;

  private ChatServiceImpl service() {
    return new ChatServiceImpl(
        chatClient, searchContentsTool, recommendContentsTool, getUserPreferenceTool,
        getPersonalizedRecommendationsTool, conversationRepository, userRepository, messageRepository);
  }

  @Test
  void chat은_존재하지_않는_사용자면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().chat(userId, null, "안녕"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void chat은_BOT_계정이_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, "user")));
    when(userRepository.findByRole(UserRole.BOT)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().chat(userId, null, "안녕"))
        .isInstanceOf(BaseException.class)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
            .isEqualTo(ErrorCode.BOT_SERVICE_UNAVAILABLE));
  }

  @Test
  void chat은_존재하지_않는_대화면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, "user")));
    when(userRepository.findByRole(UserRole.BOT)).thenReturn(Optional.of(user(UUID.randomUUID(), "bot")));
    when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().chat(userId, conversationId, "안녕"))
        .isInstanceOf(ChatNotFoundException.class);
  }

  @Test
  void chat은_AI_타입이_아닌_대화면_접근을_거부한다() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    User user = user(userId, "user");
    Conversation nonAiConversation = Conversation.builder().id(conversationId).type(ConversationType.DM).build();
    ConversationParticipant.create(nonAiConversation, user);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.findByRole(UserRole.BOT)).thenReturn(Optional.of(user(UUID.randomUUID(), "bot")));
    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(nonAiConversation));

    assertThatThrownBy(() -> service().chat(userId, conversationId, "안녕"))
        .isInstanceOf(ChatAccessDeniedException.class);
  }

  @Test
  void chat은_대화_참가자가_아니면_접근을_거부한다() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    User otherUser = user(UUID.randomUUID(), "다른사람");
    Conversation conversation = Conversation.builder().id(conversationId).type(ConversationType.AI).build();
    ConversationParticipant.create(conversation, otherUser);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, "user")));
    when(userRepository.findByRole(UserRole.BOT)).thenReturn(Optional.of(user(UUID.randomUUID(), "bot")));
    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

    assertThatThrownBy(() -> service().chat(userId, conversationId, "안녕"))
        .isInstanceOf(ChatAccessDeniedException.class);
  }

  @Test
  void chat은_신규_대화를_생성해_참가자를_등록하고_토큰을_스트리밍한_뒤_메시지를_저장하고_done으로_마무리한다() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID botId = UUID.randomUUID();
    User user = user(userId, "user");
    User bot = user(botId, "bot");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.findByRole(UserRole.BOT)).thenReturn(Optional.of(bot));
    when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
      Conversation saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", conversationId);
      return saved;
    });
    stubChatChain(Flux.just("안녕", "하세요"));

    List<ServerSentEvent<Object>> events = service().chat(userId, null, "안녕").collectList().block();

    assertThat(events).extracting(ServerSentEvent::event).containsExactly("message", "message", "done");
    verify(conversationRepository).save(any(Conversation.class));
    verify(messageRepository, times(2)).save(any(Message.class));
  }

  @Test
  void chat은_기존_대화면_새로_생성하지_않고_이어서_스트리밍한다() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    User user = user(userId, "user");
    Conversation conversation = Conversation.builder().id(conversationId).type(ConversationType.AI).build();
    ConversationParticipant.create(conversation, user);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.findByRole(UserRole.BOT)).thenReturn(Optional.of(user(UUID.randomUUID(), "bot")));
    when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
    stubChatChain(Flux.just("답변"));

    List<ServerSentEvent<Object>> events =
        service().chat(userId, conversationId, "질문").collectList().block();

    assertThat(events).extracting(ServerSentEvent::event).containsExactly("message", "done");
    verify(conversationRepository, never()).save(any());
  }

  @Test
  void chat은_스트림_중_오류가_나면_error_이벤트로_대체한다() {
    UUID userId = UUID.randomUUID();
    User user = user(userId, "user");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.findByRole(UserRole.BOT)).thenReturn(Optional.of(user(UUID.randomUUID(), "bot")));
    when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
      Conversation saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
      return saved;
    });
    stubChatChain(Flux.error(new RuntimeException("Gemini 오류")));

    List<ServerSentEvent<Object>> events = service().chat(userId, null, "안녕").collectList().block();

    assertThat(events).extracting(ServerSentEvent::event).containsExactly("error", "done");
  }

  @Test
  void chat은_툴_결과로_카드가_모이면_cards_이벤트를_함께_내려준다() {
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    User user = user(userId, "user");
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.findByRole(UserRole.BOT)).thenReturn(Optional.of(user(UUID.randomUUID(), "bot")));
    when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
      Conversation saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
      return saved;
    });
    stubChatChain(Flux.just("답변"));

    Flux<ServerSentEvent<Object>> result = service().chat(userId, null, "안녕");

    ArgumentCaptor<Map<String, Object>> contextCaptor = captureToolContext();
    Object collector = contextCaptor.getValue().get(ChatToolContext.CARD_COLLECTOR);
    ((ContentCardCollector) collector)
        .add(contentId, "제목", "thumb");

    List<ServerSentEvent<Object>> events = result.collectList().block();

    assertThat(events).extracting(ServerSentEvent::event).containsExactly("message", "cards", "done");
  }

  @Test
  void chatAnonymous는_conversationId가_없으면_새_세션ID를_발급해_done에_담는다() {
    stubChatChain(Flux.just("답변"));

    List<ServerSentEvent<Object>> events = service().chatAnonymous(null, "안녕").collectList().block();

    assertThat(events).extracting(ServerSentEvent::event).containsExactly("message", "done");
  }

  @Test
  void chatAnonymous는_conversationId가_있으면_그대로_세션ID로_사용한다() {
    UUID conversationId = UUID.randomUUID();
    stubChatChain(Flux.just("답변"));

    List<ServerSentEvent<Object>> events =
        service().chatAnonymous(conversationId, "안녕").collectList().block();

    assertThat(events).hasSize(2);
    Object doneData = events.get(1).data();
    assertThat(doneData).isInstanceOf(ChatDoneEvent.class);
    assertThat(((ChatDoneEvent) doneData)
        .conversationId()).isEqualTo(conversationId);
  }

  @Test
  void chatAnonymous는_스트림_중_오류가_나면_error_이벤트로_대체한다() {
    stubChatChain(Flux.error(new RuntimeException("Gemini 오류")));

    List<ServerSentEvent<Object>> events = service().chatAnonymous(null, "안녕").collectList().block();

    assertThat(events).extracting(ServerSentEvent::event).containsExactly("error", "done");
  }

  private void stubChatChain(Flux<String> content) {
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
    when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
    when(requestSpec.advisors(any(Consumer.class)))
        .thenReturn(requestSpec);
    when(requestSpec.stream()).thenReturn(streamResponseSpec);
    when(streamResponseSpec.content()).thenReturn(content);
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<Map<String, Object>> captureToolContext() {
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(requestSpec).toolContext(captor.capture());
    return captor;
  }

  private User user(UUID id, String username) {
    User user = User.create(username + "@test.com", username, "encoded-password");
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }
}
