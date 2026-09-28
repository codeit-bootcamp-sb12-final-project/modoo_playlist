package com.codeit.modoo_playlist.moduleapi.domain.follow.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
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

import com.codeit.modoo_playlist.core.domain.follow.entity.Follow;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.config.QuerydslConfig;
import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.FollowRepository;
import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.query.FollowListCondition.SortDirection;

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
@ContextConfiguration(classes = FollowQueryRepositoryImplTest.JpaTestConfiguration.class)
class FollowQueryRepositoryImplTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.codeit.modoo_playlist.core")
    @EnableJpaRepositories(basePackages = "com.codeit.modoo_playlist.moduleapi.domain.follow")
    @Import(QuerydslConfig.class)
    static class JpaTestConfiguration {
    }

    @Test
    void FOLLOWERS_타입은_followeeId_기준으로_필터링한다() {
        UUID target = persistUser("target1@test.com");
        UUID other = persistUser("other1@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Follow followerA = persistFollow(persistUser("followerA1@test.com"), target, t);
        Follow followerB = persistFollow(persistUser("followerB1@test.com"), target, t.plusSeconds(1));
        persistFollow(target, other, t.plusSeconds(2));
        flushAndClear();

        FollowQueryPage result = followRepository.findAllByCondition(
                condition(target, FollowListType.FOLLOWERS, null, null, 20, SortDirection.DESCENDING));

        assertThat(result.follows()).extracting(Follow::getId)
                .containsExactlyInAnyOrder(followerA.getId(), followerB.getId());
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @Test
    void FOLLOWING_타입은_followerId_기준으로_필터링한다() {
        UUID target = persistUser("target2@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Follow followingA = persistFollow(target, persistUser("followingA2@test.com"), t);
        Follow followingB = persistFollow(target, persistUser("followingB2@test.com"), t.plusSeconds(1));
        persistFollow(persistUser("stranger2@test.com"), target, t.plusSeconds(2));
        flushAndClear();

        FollowQueryPage result = followRepository.findAllByCondition(
                condition(target, FollowListType.FOLLOWING, null, null, 20, SortDirection.DESCENDING));

        assertThat(result.follows()).extracting(Follow::getId)
                .containsExactlyInAnyOrder(followingA.getId(), followingB.getId());
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @Test
    void 서로_다른_생성시각의_커서_페이지는_중복과_누락_없이_다음_페이지를_조회한다() {
        UUID target = persistUser("target3@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Follow first = persistFollow(persistUser("first3@test.com"), target, t);
        Follow second = persistFollow(persistUser("second3@test.com"), target, t.plusSeconds(1));
        Follow third = persistFollow(persistUser("third3@test.com"), target, t.plusSeconds(2));
        flushAndClear();

        FollowQueryPage firstPage = followRepository.findAllByCondition(
                condition(target, FollowListType.FOLLOWERS, null, null, 2, SortDirection.DESCENDING));
        FollowQueryPage secondPage = followRepository.findAllByCondition(
                condition(target, FollowListType.FOLLOWERS, firstPage.nextCursor(), firstPage.nextIdAfter(), 2,
                        SortDirection.DESCENDING));

        List<UUID> allIds = Stream.concat(firstPage.follows().stream(), secondPage.follows().stream())
                .map(Follow::getId)
                .toList();

        assertThat(firstPage.follows()).hasSize(2);
        assertThat(secondPage.follows()).hasSize(1);
        assertThat(allIds)
                .containsExactlyInAnyOrder(first.getId(), second.getId(), third.getId())
                .doesNotHaveDuplicates();
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(firstPage.follows()).extracting(Follow::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(secondPage.follows()).extracting(Follow::getId)
                .containsExactly(first.getId());
    }

    @Test
    void 생성시각이_같아도_id_타이브레이크로_정렬과_페이지네이션이_안정적이다() {
        UUID target = persistUser("target4@test.com");
        Instant sameInstant = Instant.parse("2026-09-14T00:00:00Z");

        Follow first = persistFollow(persistUser("first4@test.com"), target, sameInstant);
        Follow second = persistFollow(persistUser("second4@test.com"), target, sameInstant);
        Follow third = persistFollow(persistUser("third4@test.com"), target, sameInstant);
        flushAndClear();

        FollowQueryPage firstPage = followRepository.findAllByCondition(
                condition(target, FollowListType.FOLLOWERS, null, null, 2, SortDirection.DESCENDING));
        FollowQueryPage secondPage = followRepository.findAllByCondition(
                condition(target, FollowListType.FOLLOWERS, firstPage.nextCursor(), firstPage.nextIdAfter(), 2,
                        SortDirection.DESCENDING));

        List<UUID> allIds = Stream.concat(firstPage.follows().stream(), secondPage.follows().stream())
                .map(Follow::getId)
                .toList();

        assertThat(firstPage.follows()).hasSize(2);
        assertThat(secondPage.follows()).hasSize(1);
        assertThat(allIds).containsExactly(third.getId(), second.getId(), first.getId());
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 오름차순_정렬도_정상적으로_동작한다() {
        UUID target = persistUser("target5@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Follow first = persistFollow(persistUser("first5@test.com"), target, t);
        Follow second = persistFollow(persistUser("second5@test.com"), target, t.plusSeconds(1));
        flushAndClear();

        FollowQueryPage result = followRepository.findAllByCondition(
                condition(target, FollowListType.FOLLOWERS, null, null, 20, SortDirection.ASCENDING));

        assertThat(result.follows()).extracting(Follow::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void 잘못된_형식의_커서면_예외가_발생한다() {
        UUID target = UUID.randomUUID();

        assertThatThrownBy(() -> followRepository.findAllByCondition(
                condition(target, FollowListType.FOLLOWERS, "not-a-valid-instant", UUID.randomUUID(), 20,
                        SortDirection.DESCENDING)))
                .isInstanceOf(BaseException.class)
                .extracting(ex -> ((BaseException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    private Follow persistFollow(UUID followerId, UUID followeeId, Instant createdAt) {
        Follow follow = Follow.builder()
                .followerId(followerId)
                .followeeId(followeeId)
                .createdAt(createdAt)
                .build();
        entityManager.persist(follow);
        return follow;
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

    private FollowListCondition condition(
            UUID userId, FollowListType type, String cursor, UUID idAfter, int limit, SortDirection direction
    ) {
        return new FollowListCondition(userId, type, cursor, idAfter, limit, direction);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}