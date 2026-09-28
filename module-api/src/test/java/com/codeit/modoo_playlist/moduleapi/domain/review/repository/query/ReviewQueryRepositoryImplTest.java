package com.codeit.modoo_playlist.moduleapi.domain.review.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.review.entity.Review;
import com.codeit.modoo_playlist.core.domain.review.entity.ReviewStatus;
import com.codeit.modoo_playlist.infra.config.QuerydslConfig;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.ReviewRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.query.ReviewListCondition.SortDirection;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.query.ReviewListCondition.SortType;

import jakarta.persistence.EntityManager;

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
@ContextConfiguration(classes = ReviewQueryRepositoryImplTest.JpaTestConfiguration.class)
class ReviewQueryRepositoryImplTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.codeit.modoo_playlist.core")
    @EnableJpaRepositories(basePackages = "com.codeit.modoo_playlist.moduleapi.domain.review")
    @Import(QuerydslConfig.class)
    static class JpaTestConfiguration {
    }

    @Test
    void contentId_기준으로_필터링한다() {
        Content target = persistContent("target content");
        Content other = persistContent("other content");
        UUID author1 = persistUser("author1@test.com");
        UUID author2 = persistUser("author2@test.com");
        UUID author3 = persistUser("author3@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Review review1 = persistReview(target.getId(), author1, t);
        Review review2 = persistReview(target.getId(), author2, t.plusSeconds(1));
        persistReview(other.getId(), author3, t.plusSeconds(2));
        flushAndClear();

        ReviewQueryPage result = reviewRepository.findAllByCondition(
                condition(target.getId(), null, null, 20, SortDirection.DESCENDING));

        assertThat(result.reviews()).extracting(Review::getId)
                .containsExactlyInAnyOrder(review1.getId(), review2.getId());
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @Test
    void 커서_페이지네이션은_중복과_누락_없이_다음_페이지를_조회한다() {
        Content content = persistContent("content");
        UUID author1 = persistUser("author1b@test.com");
        UUID author2 = persistUser("author2b@test.com");
        UUID author3 = persistUser("author3b@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Review first = persistReview(content.getId(), author1, t);
        Review second = persistReview(content.getId(), author2, t.plusSeconds(1));
        Review third = persistReview(content.getId(), author3, t.plusSeconds(2));
        flushAndClear();

        ReviewQueryPage firstPage = reviewRepository.findAllByCondition(
                condition(content.getId(), null, null, 2, SortDirection.DESCENDING));
        ReviewQueryPage secondPage = reviewRepository.findAllByCondition(
                condition(content.getId(), firstPage.nextCursor(), firstPage.nextIdAfter(), 2,
                        SortDirection.DESCENDING));

        List<UUID> allIds = Stream.concat(firstPage.reviews().stream(), secondPage.reviews().stream())
                .map(Review::getId)
                .toList();

        assertThat(firstPage.reviews()).hasSize(2);
        assertThat(secondPage.reviews()).hasSize(1);
        assertThat(allIds)
                .containsExactlyInAnyOrder(first.getId(), second.getId(), third.getId())
                .doesNotHaveDuplicates();
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(firstPage.reviews()).extracting(Review::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(secondPage.reviews()).extracting(Review::getId)
                .containsExactly(first.getId());
    }

    @Test
    void 생성시각이_같아도_id_타이브레이크로_정렬이_안정적이다() {
        Content content = persistContent("content2");
        UUID author1 = persistUser("author1c@test.com");
        UUID author2 = persistUser("author2c@test.com");
        UUID author3 = persistUser("author3c@test.com");
        Instant sameInstant = Instant.parse("2026-09-14T00:00:00Z");

        Review first = persistReview(content.getId(), author1, sameInstant);
        Review second = persistReview(content.getId(), author2, sameInstant);
        Review third = persistReview(content.getId(), author3, sameInstant);
        flushAndClear();

        ReviewQueryPage firstPage = reviewRepository.findAllByCondition(
                condition(content.getId(), null, null, 2, SortDirection.DESCENDING));
        ReviewQueryPage secondPage = reviewRepository.findAllByCondition(
                condition(content.getId(), firstPage.nextCursor(), firstPage.nextIdAfter(), 2,
                        SortDirection.DESCENDING));

        List<UUID> allIds = Stream.concat(firstPage.reviews().stream(), secondPage.reviews().stream())
                .map(Review::getId)
                .toList();

        assertThat(allIds).containsExactly(third.getId(), second.getId(), first.getId());
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 오름차순_정렬도_정상적으로_동작한다() {
        Content content = persistContent("content3");
        UUID author1 = persistUser("author1d@test.com");
        UUID author2 = persistUser("author2d@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Review first = persistReview(content.getId(), author1, t);
        Review second = persistReview(content.getId(), author2, t.plusSeconds(1));
        flushAndClear();

        ReviewQueryPage result = reviewRepository.findAllByCondition(
                condition(content.getId(), null, null, 20, SortDirection.ASCENDING));

        assertThat(result.reviews()).extracting(Review::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void 잘못된_형식의_커서면_DateTimeParseException이_발생한다() {

        UUID contentId = UUID.randomUUID();

        assertThatThrownBy(() -> reviewRepository.findAllByCondition(
                condition(contentId, "not-a-valid-instant", UUID.randomUUID(), 20, SortDirection.DESCENDING)))
                .isInstanceOf(DateTimeParseException.class);
    }

    private Content persistContent(String title) {
        Instant timestamp = Instant.parse("2026-09-14T00:00:00Z");
        Content content = Content.builder()
                .type(ContentType.MOVIE)
                .title(title)
                .averageRating(BigDecimal.ZERO)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
        entityManager.persist(content);
        return content;
    }

    private Review persistReview(UUID contentId, UUID authorId, Instant createdAt) {
        Review review = Review.builder()
                .contentId(contentId)
                .authorId(authorId)
                .text("review text")
                .rating(new BigDecimal("4.0"))
                .status(ReviewStatus.COMPLETED)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        entityManager.persist(review);
        return review;
    }

    private UUID persistUser(String email) {
        UUID id = UUID.randomUUID();
        String username = email.substring(0, email.indexOf('@'));
        jdbcTemplate.update(
                "INSERT INTO users(id, email, username, role, locked, created_at, updated_at) "
                        + "VALUES (UUID_TO_BIN(?), ?, ?, 'USER', false, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))",
                id.toString(), email, username
        );
        return id;
    }

    private ReviewListCondition condition(
            UUID contentId, String cursor, UUID idAfter, int limit, SortDirection sortDirection
    ) {
        return new ReviewListCondition(contentId, cursor, idAfter, limit, SortType.CREATED_AT, sortDirection);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}