package com.codeit.modoo_playlist.moduleapi.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationParticipant;
import com.codeit.modoo_playlist.core.domain.conversation.entity.ConversationType;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.infra.config.QuerydslConfig;
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

/**
 * ChatServiceImpl.deleteConversation()에서 실제로 발생했던
 * TransientPropertyValueException(participants 지연 로딩 후 delete)을 재현·검증한다.
 * Mockito 단위 테스트로는 실제 Hibernate flush가 없어 재현되지 않는 버그라 별도로 둔다.
 */
@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = ConversationRepositoryDeleteConversationTest.JpaConfig.class)
class ConversationRepositoryDeleteConversationTest {

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
  void participants를_먼저_로딩하고_delete하면_TransientPropertyValueException이_발생한다() {
    User requester = persistUser();
    Conversation conversation = persistConversation(requester);
    entityManager.flush();
    entityManager.clear();

    Conversation loaded = conversationRepository.findById(conversation.getId()).orElseThrow();
    // ChatServiceImpl의 옛 코드가 하던 것과 동일 — 지연 로딩된 participants를 관리 상태로 끌어옴
    loaded.getParticipants().stream().findAny();
    conversationRepository.delete(loaded);

    // Hibernate가 JPA 스펙에 맞춰 IllegalStateException으로 감싸서 던진다.
    assertThatThrownBy(() -> entityManager.flush())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("TransientPropertyValueException");
  }
  private User persistUser() {
    User user = User.create("requester-" + UUID.randomUUID() + "@test.com", "requester", "encoded-password");
    ReflectionTestUtils.setField(user, "createdAt", BASE);
    ReflectionTestUtils.setField(user, "updatedAt", BASE);
    entityManager.persist(user);
    return user;
  }

  private Conversation persistConversation(User participant) {
    Conversation conversation = Conversation.builder()
        .type(ConversationType.AI)
        .createdAt(BASE)
        .updatedAt(BASE)
        .build();
    entityManager.persist(conversation);
    ConversationParticipant conversationParticipant = ConversationParticipant.create(conversation, participant);
    ReflectionTestUtils.setField(conversationParticipant, "createdAt", BASE);
    entityManager.persist(conversationParticipant);
    return conversation;
  }
}
