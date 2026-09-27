package com.codeit.modoo_playlist.modulebatch.user.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.modoo_playlist.modulebatch.user.model.UserDeletionTarget;
import com.codeit.modoo_playlist.modulebatch.user.reader.UserDeletionReader;
import com.codeit.modoo_playlist.modulebatch.user.writer.UserDeletionWriter;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

@MybatisTest(properties = {
    "spring.datasource.url=jdbc:tc:mysql:8.4.7:///modoo_mysql",
    "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
    "spring.datasource.username=test",
    "spring.datasource.password=test",
    "spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=file:../infra/src/main/resources/schema.sql",
    "spring.test.database.replace=NONE",
    "mybatis.mapper-locations=classpath*:mapper/**/*.xml"
})
@ContextConfiguration(classes = UserDeletionMapperMySqlTest.MyBatisTestConfiguration.class)
class UserDeletionMapperMySqlTest {

  private static final Instant CUTOFF = Instant.parse("2026-09-22T00:00:00Z");

  @Autowired
  private UserDeletionMapper mapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @MapperScan("com.codeit.modoo_playlist.modulebatch.user.persistence")
  static class MyBatisTestConfiguration {

  }

  @Test
  void findsOnlyWithdrawnUsersWhoseRetentionHasElapsed() {
    String eligibleUserId = insertUser("eligible@example.com", "USER", CUTOFF.minusSeconds(1));
    insertUser("recent@example.com", "USER", CUTOFF.plusSeconds(1));
    insertUser("active@example.com", "USER", null);
    insertUser("bot@example.com", "BOT", CUTOFF.minusSeconds(1));

    List<UserDeletionTarget> targets = mapper.findDeletionTargets(CUTOFF, null, 100);

    assertThat(targets).extracting(UserDeletionTarget::userId).containsExactly(eligibleUserId);
  }

  @Test
  void deletesRestrictedDataBeforeDeletingUser() {
    String userId = insertUser("withdrawn@example.com", "USER", CUTOFF.minusSeconds(1));
    String otherUserId = insertUser("other@example.com", "USER", null);
    String contentId = insertContent();
    insertMessage(userId, otherUserId);
    insertWatchingSession(userId, contentId);
    insertReview(userId, contentId);

    assertThat(mapper.lockDeletionTarget(userId, CUTOFF)).isEqualTo(userId);
    assertThat(mapper.deleteMessages(userId)).isEqualTo(1);
    assertThat(mapper.deleteWatchingSessions(userId)).isEqualTo(1);
    assertThat(mapper.deleteReviews(userId)).isEqualTo(1);
    assertThat(mapper.deleteUser(userId, CUTOFF)).isEqualTo(1);

    assertThat(countById("users", userId)).isZero();
    assertThat(countByUserId("messages", "sender_id", userId)).isZero();
    assertThat(countByUserId("watching_sessions", "watcher_id", userId)).isZero();
    assertThat(countByUserId("reviews", "author_id", userId)).isZero();
  }

  @Test
  void deletesOnlyOneWithdrawnUserTenSecondsAfterWithdrawal() {
    Instant withdrawnAt = Instant.parse("2026-09-22T02:00:00Z");
    List<String> userIds = new ArrayList<>();
    for (int index = 0; index < 10; index++) {
      userIds.add(insertUser("user" + index + "@example.com", "USER", null));
    }
    String withdrawnUserId = userIds.get(4);
    withdrawUser(withdrawnUserId, withdrawnAt);
    Instant deletionCutoff = withdrawnAt.plusSeconds(10);

    UserDeletionReader reader = new UserDeletionReader(mapper, deletionCutoff, 100, 100);
    List<UserDeletionTarget> targets = new ArrayList<>();
    UserDeletionTarget target;
    while ((target = reader.read()) != null) {
      targets.add(target);
    }

    assertThat(targets).extracting(UserDeletionTarget::userId)
        .containsExactly(withdrawnUserId);

    new UserDeletionWriter(mapper, deletionCutoff).write(new Chunk<>(targets));

    assertThat(countById("users", withdrawnUserId)).isZero();
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class))
        .isEqualTo(9);
  }

  private String insertUser(String email, String role, Instant deletedAt) {
    String id = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO users (id, email, username, role, deleted_at) "
            + "VALUES (UUID_TO_BIN(?), ?, ?, ?, ?)",
        id,
        email,
        email,
        role,
        deletedAt == null ? null : Timestamp.from(deletedAt)
    );
    return id;
  }

  private String insertContent() {
    String id = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO contents (id, type, title) VALUES (UUID_TO_BIN(?), 'MOVIE', 'test')",
        id
    );
    return id;
  }

  private void withdrawUser(String userId, Instant withdrawnAt) {
    jdbcTemplate.update(
        "UPDATE users SET deleted_at = ? WHERE id = UUID_TO_BIN(?)",
        Timestamp.from(withdrawnAt),
        userId
    );
  }

  private void insertMessage(String senderId, String receiverId) {
    jdbcTemplate.update(
        "INSERT INTO messages (id, sender_id, receiver_id, type, message) "
            + "VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?), 'DIRECT', 'test')",
        UUID.randomUUID().toString(),
        senderId,
        receiverId
    );
  }

  private void insertWatchingSession(String userId, String contentId) {
    jdbcTemplate.update(
        "INSERT INTO watching_sessions (id, watcher_id, content_id) "
            + "VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?))",
        UUID.randomUUID().toString(),
        userId,
        contentId
    );
  }

  private void insertReview(String userId, String contentId) {
    jdbcTemplate.update(
        "INSERT INTO reviews (id, content_id, author_id, text, rating, status) "
            + "VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?), 'test', 5.0, 'COMPLETED')",
        UUID.randomUUID().toString(),
        contentId,
        userId
    );
  }

  private long countById(String table, String id) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + table + " WHERE id = UUID_TO_BIN(?)",
        Long.class,
        id
    );
  }

  private long countByUserId(String table, String column, String userId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = UUID_TO_BIN(?)",
        Long.class,
        userId
    );
  }
}
