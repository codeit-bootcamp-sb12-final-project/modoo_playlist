package com.codeit.modoo_playlist.moduleapi.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationParticipant;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationType;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.message.entity.MessageType;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.common.dto.base.SliceCursorRequest;
import com.codeit.modoo_playlist.infra.config.QuerydslConfig;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ChatConversationCursorResponse;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ChatConversationSummaryDto;
import com.codeit.modoo_playlist.moduleapi.domain.conversation.repository.ConversationRepository;
import com.codeit.modoo_playlist.moduleapi.mapper.ConversationMapper;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = ConversationRepositoryFindAiConversationsTest.JpaConfig.class)
class ConversationRepositoryFindAiConversationsTest {

  private static final Instant BASE = Instant.parse("2026-09-01T00:00:00Z");

  @Configuration(proxyBeanMethods = false)
  @EntityScan(basePackages = "com.codeit.modoo_playlist.core.domain")
  @EnableJpaRepositories(basePackageClasses = ConversationRepository.class)
  @Import(QuerydslConfig.class)
  static class JpaConfig {

    @Bean
    ConversationMapper conversationMapper() {
      return mock(ConversationMapper.class);
    }
  }

  @Autowired private ConversationRepository conversationRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void AI_타입이고_요청자가_참여한_대화만_제목과_함께_조회한다() {
    User requester = persistUser("requester@test.com");
    User bot = persistUser("bot@test.com");
    User other = persistUser("other@test.com");

    Conversation aiConversation = persistConversation(ConversationType.AI, BASE.plusSeconds(10), requester);
    persistMessage(aiConversation, bot, requester, "안녕하세요! 결정된 초기 안내 메시지", BASE.plusSeconds(9));
    persistMessage(aiConversation, requester, bot, "로맨스 영화 추천해줘", BASE.plusSeconds(11));
    persistMessage(aiConversation, bot, requester, "이런 작품은 어때요?", BASE.plusSeconds(12));
    persistMessage(aiConversation, requester, bot, "다른 것도 알려줘", BASE.plusSeconds(13));

    persistConversation(ConversationType.DM, BASE.plusSeconds(20), requester);
    persistConversation(ConversationType.AI, BASE.plusSeconds(30), other);

    flushAndClear();

    ChatConversationCursorResponse response = conversationRepository.findAiConversations(
        requester.getId(), new SliceCursorRequest(null, null, 10, null, null));

    assertThat(response.data()).hasSize(1);
    ChatConversationSummaryDto summary = response.data().get(0);
    assertThat(summary.id()).isEqualTo(aiConversation.getId());
    assertThat(summary.title()).isEqualTo("로맨스 영화 추천해줘");
    assertThat(response.totalCount()).isEqualTo(1);
    assertThat(response.hasNext()).isFalse();
  }

  @Test
  void 요청자가_보낸_메시지가_없으면_제목은_새_대화이다() {
    User requester = persistUser("requester@test.com");
    Conversation conversation = persistConversation(ConversationType.AI, BASE, requester);

    flushAndClear();

    ChatConversationCursorResponse response = conversationRepository.findAiConversations(
        requester.getId(), new SliceCursorRequest(null, null, 10, null, null));

    assertThat(response.data()).extracting(ChatConversationSummaryDto::title).containsExactly("새 대화");
  }

  @Test
  void 첫_메시지가_50자를_넘으면_50자로_자른다() {
    User requester = persistUser("requester@test.com");
    User bot = persistUser("bot@test.com");
    Conversation conversation = persistConversation(ConversationType.AI, BASE, requester);
    String longMessage = "가".repeat(60);
    persistMessage(conversation, requester, bot, longMessage, BASE.plusSeconds(1));

    flushAndClear();

    ChatConversationCursorResponse response = conversationRepository.findAiConversations(
        requester.getId(), new SliceCursorRequest(null, null, 10, null, null));

    assertThat(response.data().get(0).title()).isEqualTo("가".repeat(50));
  }

  @Test
  void 커서로_다음_페이지를_가져올_수_있다() {
    User requester = persistUser("requester@test.com");
    Conversation first = persistConversation(ConversationType.AI, BASE.plusSeconds(1), requester);
    Conversation second = persistConversation(ConversationType.AI, BASE.plusSeconds(2), requester);
    Conversation third = persistConversation(ConversationType.AI, BASE.plusSeconds(3), requester);

    flushAndClear();

    ChatConversationCursorResponse firstPage = conversationRepository.findAiConversations(
        requester.getId(), new SliceCursorRequest(null, null, 2, null, null));

    assertThat(firstPage.data()).extracting(ChatConversationSummaryDto::id)
        .containsExactly(third.getId(), second.getId());
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(firstPage.totalCount()).isEqualTo(3);
    assertThat(firstPage.nextCursor()).isNotNull();
    assertThat(firstPage.nextIdAfter()).isEqualTo(second.getId());

    ChatConversationCursorResponse secondPage = conversationRepository.findAiConversations(
        requester.getId(),
        new SliceCursorRequest(firstPage.nextCursor(), firstPage.nextIdAfter(), 2, null, null));

    assertThat(secondPage.data()).extracting(ChatConversationSummaryDto::id)
        .containsExactly(first.getId());
    assertThat(secondPage.hasNext()).isFalse();
  }

  private User persistUser(String email) {
    User user = User.create(email, "user-" + UUID.randomUUID(), "encoded-password");
    ReflectionTestUtils.setField(user, "createdAt", BASE);
    ReflectionTestUtils.setField(user, "updatedAt", BASE);
    entityManager.persist(user);
    return user;
  }

  private Conversation persistConversation(ConversationType type, Instant createdAt, User participant) {
    Conversation conversation = Conversation.builder()
        .type(type)
        .createdAt(createdAt)
        .updatedAt(createdAt)
        .build();
    entityManager.persist(conversation);
    ConversationParticipant conversationParticipant = ConversationParticipant.create(conversation, participant);
    ReflectionTestUtils.setField(conversationParticipant, "createdAt", createdAt);
    entityManager.persist(conversationParticipant);
    return conversation;
  }

  private void persistMessage(
      Conversation conversation, User sender, User receiver, String message, Instant createdAt
  ) {
    entityManager.persist(Message.builder()
        .conversation(conversation)
        .sender(sender)
        .receiver(receiver)
        .type(MessageType.AI)
        .message(message)
        .createdAt(createdAt)
        .build());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
