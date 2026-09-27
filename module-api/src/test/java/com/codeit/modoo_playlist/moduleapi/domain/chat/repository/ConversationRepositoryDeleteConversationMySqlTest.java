package com.codeit.modoo_playlist.moduleapi.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * conversation_participants.conversation_id는 운영 schema.sql에서
 * ON DELETE CASCADE로 선언돼 있다(Conversation.participants 엔티티 매핑에는
 * cascade=REMOVE가 없음). H2(ddl-auto) 스키마는 이 DB 레벨 cascade를
 * 포함하지 않아 검증이 불가능하므로, 실제 schema.sql을 적용하는 Testcontainers
 * MySQL로 검증한다.
 */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:tc:mysql:8.4.7:///modoo_mysql",
    "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
    "spring.datasource.username=test",
    "spring.datasource.password=test",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=file:../infra/src/main/resources/schema.sql",
    "spring.test.database.replace=NONE"
})
@ContextConfiguration(classes = ConversationRepositoryDeleteConversationMySqlTest.JpaTestConfiguration.class)
class ConversationRepositoryDeleteConversationMySqlTest {

  private static final Instant BASE = Instant.parse("2026-09-01T00:00:00Z");

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @EntityScan(basePackages = "com.codeit.modoo_playlist.core.domain")
  @EnableJpaRepositories(basePackageClasses = ConversationRepository.class)
  @Import(QuerydslConfig.class)
  static class JpaTestConfiguration {

    @Bean
    ConversationMapper conversationMapper() {
      return mock(ConversationMapper.class);
    }
  }

  @Autowired private ConversationRepository conversationRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void existsParticipant로_확인_후_delete하면_참여자행이_DB_cascade로_함께_삭제된다() {
    User requester = persistUser();
    Conversation conversation = persistConversation(requester);
    entityManager.flush();
    entityManager.clear();

    Conversation loaded = conversationRepository.findById(conversation.getId()).orElseThrow();
    boolean isParticipant =
        conversationRepository.existsParticipant(conversation.getId(), requester.getId());
    assertThat(isParticipant).isTrue();

    assertThatCode(() -> {
      conversationRepository.delete(loaded);
      entityManager.flush();
    }).doesNotThrowAnyException();

    assertThat(conversationRepository.findById(conversation.getId())).isEmpty();
    Integer remainingParticipants = jdbcTemplate.queryForObject(
        "select count(*) from conversation_participants where conversation_id = UUID_TO_BIN(?)",
        Integer.class, conversation.getId().toString());
    assertThat(remainingParticipants).isZero();
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
