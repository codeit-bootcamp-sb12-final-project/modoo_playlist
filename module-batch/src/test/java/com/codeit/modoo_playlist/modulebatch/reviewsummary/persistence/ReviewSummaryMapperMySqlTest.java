package com.codeit.modoo_playlist.modulebatch.reviewsummary.persistence;

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

import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryCandidate;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryReviewRow;

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
@ContextConfiguration(classes = ReviewSummaryMapperMySqlTest.MyBatisTestConfiguration.class)
class ReviewSummaryMapperMySqlTest {

  @Autowired private ReviewSummaryMapper mapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @MapperScan("com.codeit.modoo_playlist.modulebatch.reviewsummary.persistence")
  static class MyBatisTestConfiguration {
  }

  @Test
  void 요약이_없는_콘텐츠는_후보에_포함된다() {
    String contentId = insertContent("영화A", 3);
    insertReviews(contentId, 3, Instant.parse("2026-01-01T00:00:00Z"));

    List<ReviewSummaryCandidate> candidates = mapper.findContentsNeedingSummary(3, null, 10);

    assertThat(candidates).extracting(ReviewSummaryCandidate::contentId).contains(contentId);
  }

  @Test
  void reviewCount가_minReviews_미만이면_후보에서_제외된다() {
    String contentId = insertContent("영화B", 1);
    insertReviews(contentId, 1, Instant.parse("2026-01-01T00:00:00Z"));

    List<ReviewSummaryCandidate> candidates = mapper.findContentsNeedingSummary(3, null, 10);

    assertThat(candidates).extracting(ReviewSummaryCandidate::contentId).doesNotContain(contentId);
  }

  @Test
  void 요약이_리뷰보다_최신이면_후보에서_제외된다() {
    String contentId = insertContent("영화C", 3);
    insertReviews(contentId, 3, Instant.parse("2026-01-01T00:00:00Z"));
    insertSummary(contentId, "기존 요약", Instant.parse("2026-06-01T00:00:00Z"));

    List<ReviewSummaryCandidate> candidates = mapper.findContentsNeedingSummary(3, null, 10);

    assertThat(candidates).extracting(ReviewSummaryCandidate::contentId).doesNotContain(contentId);
  }

  @Test
  void 요약_생성_이후_리뷰가_갱신되면_다시_후보가_된다() {
    String contentId = insertContent("영화D", 3);
    insertReviews(contentId, 2, Instant.parse("2026-01-01T00:00:00Z"));
    insertReview(contentId, "새 리뷰", new BigDecimal("4.0"), Instant.parse("2026-08-01T00:00:00Z"));
    insertSummary(contentId, "기존 요약", Instant.parse("2026-06-01T00:00:00Z"));

    List<ReviewSummaryCandidate> candidates = mapper.findContentsNeedingSummary(3, null, 10);

    assertThat(candidates).extracting(ReviewSummaryCandidate::contentId).contains(contentId);
  }

  @Test
  void keyset_커서_이후의_콘텐츠만_반환하고_이미_지나온_콘텐츠는_반환하지_않는다() {
    String contentA = insertContent("영화E", 3);
    insertReviews(contentA, 3, Instant.parse("2026-01-01T00:00:00Z"));
    String contentB = insertContent("영화F", 3);
    insertReviews(contentB, 3, Instant.parse("2026-01-01T00:00:00Z"));

    List<ReviewSummaryCandidate> firstPage = mapper.findContentsNeedingSummary(3, null, 10);
    assertThat(firstPage).extracting(ReviewSummaryCandidate::contentId).contains(contentA, contentB);

    String firstId = firstPage.get(0).contentId();
    String secondId = firstPage.get(1).contentId();

    List<ReviewSummaryCandidate> nextPage = mapper.findContentsNeedingSummary(3, firstId, 10);

    assertThat(nextPage).extracting(ReviewSummaryCandidate::contentId)
        .contains(secondId)
        .doesNotContain(firstId);
  }

  @Test
  void 콘텐츠당_최신_리뷰_N건만_최신순으로_반환한다() {
    String contentId = insertContent("영화G", 5);
    insertReview(contentId, "가장오래됨", new BigDecimal("1.0"), Instant.parse("2026-01-01T00:00:00Z"));
    insertReview(contentId, "중간1", new BigDecimal("2.0"), Instant.parse("2026-02-01T00:00:00Z"));
    insertReview(contentId, "중간2", new BigDecimal("3.0"), Instant.parse("2026-03-01T00:00:00Z"));
    insertReview(contentId, "최신1", new BigDecimal("4.0"), Instant.parse("2026-04-01T00:00:00Z"));
    insertReview(contentId, "최신2", new BigDecimal("5.0"), Instant.parse("2026-05-01T00:00:00Z"));

    List<ReviewSummaryReviewRow> rows = mapper.findReviewsForContents(List.of(contentId), 2);

    assertThat(rows).extracting(ReviewSummaryReviewRow::text).containsExactly("최신2", "최신1");
  }

  @Test
  void upsertSummary는_최초_삽입과_이후_갱신을_모두_처리한다() {
    String contentId = insertContent("영화H", 3);
    insertReviews(contentId, 3, Instant.parse("2026-01-01T00:00:00Z"));

    mapper.upsertSummary(contentId, "첫 요약");
    assertThat(summaryOf(contentId)).isEqualTo("첫 요약");

    mapper.upsertSummary(contentId, "갱신된 요약");
    assertThat(summaryOf(contentId)).isEqualTo("갱신된 요약");
    assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM content_review_summaries WHERE content_id = UUID_TO_BIN(?)",
        Integer.class, contentId)).isEqualTo(1);
  }

  private void insertReviews(String contentId, int count, Instant updatedAt) {
    for (int i = 0; i < count; i++) {
      insertReview(contentId, "리뷰" + i, new BigDecimal("4.0"), updatedAt);
    }
  }

  private String insertContent(String title, int reviewCount) {
    String contentId = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO contents (id, type, title, review_count) VALUES (UUID_TO_BIN(?), 'MOVIE', ?, ?)",
        contentId, title, reviewCount);
    return contentId;
  }

  private void insertReview(String contentId, String text, BigDecimal rating, Instant updatedAt) {
    String reviewId = UUID.randomUUID().toString();
    String authorId = insertUser();
    jdbcTemplate.update(
        "INSERT INTO reviews (id, content_id, author_id, text, rating, status, updated_at) "
            + "VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?), ?, ?, 'COMPLETED', ?)",
        reviewId, contentId, authorId, text, rating, Timestamp.from(updatedAt));
  }

  private String insertUser() {
    String userId = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO users (id, email, username) VALUES (UUID_TO_BIN(?), ?, ?)",
        userId, userId + "@test.com", "user-" + userId.substring(0, 8));
    return userId;
  }

  private void insertSummary(String contentId, String summary, Instant updatedAt) {
    jdbcTemplate.update(
        "INSERT INTO content_review_summaries (content_id, summary, updated_at) "
            + "VALUES (UUID_TO_BIN(?), ?, ?)",
        contentId, summary, Timestamp.from(updatedAt));
  }

  private String summaryOf(String contentId) {
    return jdbcTemplate.queryForObject(
        "SELECT summary FROM content_review_summaries WHERE content_id = UUID_TO_BIN(?)",
        String.class, contentId);
  }
}
