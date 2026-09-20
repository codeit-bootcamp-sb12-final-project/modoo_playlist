package com.codeit.modoo_playlist.modulebatch.recommendation.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.codeit.modoo_playlist.modulebatch.recommendation.model.InteractionTagSignal;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.PreferenceTagRow;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.SimilarityRow;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.TagContentCount;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.TagName;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.UserTagScore;

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
@ContextConfiguration(classes = RecommendationRecalcMapperMySqlTest.MyBatisTestConfiguration.class)
class RecommendationRecalcMapperMySqlTest {

  @Autowired private RecommendationRecalcMapper mapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @MapperScan("com.codeit.modoo_playlist.modulebatch.recommendation.persistence")
  static class MyBatisTestConfiguration {
  }

  @Test
  void findInteractionTagSignals는_정상_사용자의_LIKE_DISLIKE류만_반환한다() {
    String tagId = insertTag(5);
    String contentId = insertContent(tagId);

    String normalUser = insertUser("USER", false);
    insertInteraction(normalUser, contentId, "LIKE", null, 1);

    String botUser = insertUser("BOT", false);
    insertInteraction(botUser, contentId, "LIKE", null, 1);

    String deletedUser = insertUser("USER", true);
    insertInteraction(deletedUser, contentId, "LIKE", null, 1);

    insertInteraction(normalUser, contentId, "VIEW", null, 3);

    List<InteractionTagSignal> signals = mapper.findInteractionTagSignals();

    assertThat(signals).extracting(InteractionTagSignal::userId).containsOnly(normalUser);
    assertThat(signals).hasSize(1);
    assertThat(signals.get(0).tagId()).isEqualTo(tagId);
  }

  @Test
  void findTagContentCounts는_태그의_content_count를_그대로_반환한다() {
    String tagId = insertTag(7);

    List<TagContentCount> counts = mapper.findTagContentCounts();

    assertThat(counts).filteredOn(c -> c.tagId().equals(tagId))
        .extracting(TagContentCount::contentCount)
        .containsExactly(7);
  }

  @Test
  void countActiveContents는_삭제되지_않은_콘텐츠만_센다() {
    String tagId = insertTag(1);
    insertContent(tagId);
    String deletedContentId = insertContent(tagId);
    jdbcTemplate.update(
        "UPDATE contents SET deleted_at = ? WHERE id = UUID_TO_BIN(?)",
        Timestamp.from(Instant.now()), deletedContentId);

    long before = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM contents WHERE deleted_at IS NULL", Long.class);

    assertThat(mapper.countActiveContents()).isEqualTo(before);
  }

  @Test
  void upsertPreferenceTags는_최초_삽입과_갱신을_모두_처리한다() {
    String tagId = insertTag(1);
    String contentId = insertContent(tagId);
    String userId = insertUser("USER", false);
    insertInteraction(userId, contentId, "LIKE", null, 1);
    Instant signalAt = Instant.parse("2026-01-01T00:00:00Z");

    mapper.upsertPreferenceTags(List.of(new PreferenceTagRow(userId, tagId, 3.0, 2.5, signalAt)));
    assertThat(scoreOf(userId, tagId)).isEqualByComparingTo("2.5000");

    mapper.upsertPreferenceTags(List.of(new PreferenceTagRow(userId, tagId, 6.0, 5.0, signalAt)));
    assertThat(scoreOf(userId, tagId)).isEqualByComparingTo("5.0000");
    assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user_preference_tags WHERE user_id = UUID_TO_BIN(?) AND tag_id = UUID_TO_BIN(?)",
        Integer.class, userId, tagId)).isEqualTo(1);
  }

  @Test
  void findAllPreferenceScores는_score가_0보다_큰_행만_반환한다() {
    String tagId = insertTag(1);
    String contentId = insertContent(tagId);
    String userId = insertUser("USER", false);
    insertInteraction(userId, contentId, "LIKE", null, 1);
    mapper.upsertPreferenceTags(List.of(
        new PreferenceTagRow(userId, tagId, 1.0, 1.0, Instant.now())
    ));

    String zeroScoreTag = insertTag(1);
    insertContent(zeroScoreTag);
    mapper.upsertPreferenceTags(List.of(
        new PreferenceTagRow(userId, zeroScoreTag, 0.0, 0.0, Instant.now())
    ));

    List<UserTagScore> scores = mapper.findAllPreferenceScores();

    assertThat(scores).extracting(UserTagScore::tagId).contains(tagId).doesNotContain(zeroScoreTag);
  }

  @Test
  void findTagNames는_태그ID와_이름을_반환한다() {
    String tagId = insertTagNamed("코미디");

    List<TagName> names = mapper.findTagNames();

    assertThat(names).filteredOn(t -> t.tagId().equals(tagId))
        .extracting(TagName::name).containsExactly("코미디");
  }

  @Test
  void upsertSimilarities와_deleteAllSimilarities가_동작한다() {
    String userA = insertUser("USER", false);
    String userB = insertUser("USER", false);
    mapper.upsertSimilarities(List.of(
        new SimilarityRow(userA, userB, 0.9, "액션", Instant.now())
    ));
    assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user_similarities", Integer.class)).isEqualTo(1);

    mapper.deleteAllSimilarities();

    assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user_similarities", Integer.class)).isZero();
  }

  private BigDecimal scoreOf(String userId, String tagId) {
    return jdbcTemplate.queryForObject(
        "SELECT score FROM user_preference_tags WHERE user_id = UUID_TO_BIN(?) AND tag_id = UUID_TO_BIN(?)",
        BigDecimal.class, userId, tagId);
  }

  private String insertUser(String role, boolean deleted) {
    String userId = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO users (id, email, username, role, deleted_at) VALUES (UUID_TO_BIN(?), ?, ?, ?, ?)",
        userId, userId + "@test.com", "user-" + userId.substring(0, 8), role,
        deleted ? Timestamp.from(Instant.now()) : null);
    return userId;
  }

  private String insertTag(int contentCount) {
    return insertTagNamed("태그-" + UUID.randomUUID(), contentCount);
  }

  private String insertTagNamed(String name) {
    return insertTagNamed(name, 1);
  }

  private String insertTagNamed(String name, int contentCount) {
    String tagId = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO tags (id, name, kind, content_count) VALUES (UUID_TO_BIN(?), ?, 'GENRE', ?)",
        tagId, name, contentCount);
    return tagId;
  }

  private String insertContent(String tagId) {
    String contentId = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO contents (id, type, title) VALUES (UUID_TO_BIN(?), 'MOVIE', ?)",
        contentId, "영화-" + contentId);
    jdbcTemplate.update(
        "INSERT INTO content_tags (tag_id, content_id, source) VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), 'OPENAPI')",
        tagId, contentId);
    return contentId;
  }

  private void insertInteraction(String userId, String contentId, String type, BigDecimal value, int occurrenceCount) {
    jdbcTemplate.update(
        "INSERT INTO user_content_interactions (id, user_id, content_id, type, value, occurrence_count) "
            + "VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?), ?, ?, ?)",
        UUID.randomUUID().toString(), userId, contentId, type, value, occurrenceCount);
  }
}
