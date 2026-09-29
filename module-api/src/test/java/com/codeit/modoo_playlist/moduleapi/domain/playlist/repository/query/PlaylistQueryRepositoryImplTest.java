package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import com.codeit.modoo_playlist.core.domain.playlist.entity.GeneratedBy;
import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistSubscription;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistSubscriptionId;
import com.codeit.modoo_playlist.infra.config.QuerydslConfig;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query.PlaylistListCondition.SortDirection;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query.PlaylistListCondition.SortType;

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
@ContextConfiguration(classes = PlaylistQueryRepositoryImplTest.JpaTestConfiguration.class)
class PlaylistQueryRepositoryImplTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.codeit.modoo_playlist.core")
    @EnableJpaRepositories(basePackages = "com.codeit.modoo_playlist.moduleapi.domain.playlist")
    @Import(QuerydslConfig.class)
    static class JpaTestConfiguration {
    }

    @Test
    void ownerId_기준으로_필터링한다() {
        UUID owner = persistUser("owner1@test.com");
        UUID other = persistUser("other1@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Playlist mine1 = persistPlaylist(owner, "내 플리 1", "설명", GeneratedBy.USER, 0, t, t);
        Playlist mine2 = persistPlaylist(owner, "내 플리 2", "설명", GeneratedBy.USER, 0, t.plusSeconds(1), t.plusSeconds(1));
        persistPlaylist(other, "남의 플리", "설명", GeneratedBy.USER, 0, t.plusSeconds(2), t.plusSeconds(2));
        flushAndClear();

        PlaylistQueryPage result = playlistRepository.findAllByCondition(
                condition(owner, null, null, null, null, 20, SortType.CREATED_AT, SortDirection.DESCENDING));

        assertThat(result.playlists()).extracting(Playlist::getId)
                .containsExactlyInAnyOrder(mine1.getId(), mine2.getId());
        assertThat(result.totalCount()).isEqualTo(2);
    }

    @Test
    void keyword로_제목을_대소문자_구분없이_검색한다() {
        UUID owner = persistUser("owner2@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Playlist matched1 = persistPlaylist(owner, "Rainy Day Movies", "설명", GeneratedBy.USER, 0, t, t);
        Playlist matched2 = persistPlaylist(owner, "rainy afternoon", "설명", GeneratedBy.USER, 0, t.plusSeconds(1),
                t.plusSeconds(1));
        persistPlaylist(owner, "Sunny Picks", "설명", GeneratedBy.USER, 0, t.plusSeconds(2), t.plusSeconds(2));
        flushAndClear();

        PlaylistQueryPage result = playlistRepository.findAllByCondition(
                condition(null, null, "RAINY", null, null, 20, SortType.CREATED_AT, SortDirection.DESCENDING));

        assertThat(result.playlists()).extracting(Playlist::getId)
                .containsExactlyInAnyOrder(matched1.getId(), matched2.getId());
    }

    @Test
    void subscriberId로_구독한_플레이리스트만_필터링한다() {
        UUID owner = persistUser("owner3@test.com");
        UUID subscriber = persistUser("subscriber3@test.com");
        UUID other = persistUser("other3@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Playlist subscribed = persistPlaylist(owner, "구독한 플리", "설명", GeneratedBy.USER, 1, t, t);
        Playlist notSubscribed = persistPlaylist(owner, "구독 안한 플리", "설명", GeneratedBy.USER, 0, t.plusSeconds(1),
                t.plusSeconds(1));
        persistSubscription(subscribed.getId(), subscriber, t);
        persistSubscription(notSubscribed.getId(), other, t);
        flushAndClear();

        PlaylistQueryPage result = playlistRepository.findAllByCondition(
                condition(null, subscriber, null, null, null, 20, SortType.CREATED_AT, SortDirection.DESCENDING));

        assertThat(result.playlists()).extracting(Playlist::getId)
                .containsExactly(subscribed.getId());
    }

    @Test
    void CREATED_AT_기준_커서_페이지네이션은_중복과_누락_없이_동작한다() {
        UUID owner = persistUser("owner4@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Playlist first = persistPlaylist(owner, "플리1", "설명", GeneratedBy.USER, 0, t, t);
        Playlist second = persistPlaylist(owner, "플리2", "설명", GeneratedBy.USER, 0, t.plusSeconds(1), t.plusSeconds(1));
        Playlist third = persistPlaylist(owner, "플리3", "설명", GeneratedBy.USER, 0, t.plusSeconds(2), t.plusSeconds(2));
        flushAndClear();

        PlaylistQueryPage firstPage = playlistRepository.findAllByCondition(
                condition(owner, null, null, null, null, 2, SortType.CREATED_AT, SortDirection.DESCENDING));
        PlaylistQueryPage secondPage = playlistRepository.findAllByCondition(
                condition(owner, null, null, firstPage.nextCursor(), firstPage.nextIdAfter(), 2, SortType.CREATED_AT,
                        SortDirection.DESCENDING));

        List<UUID> allIds = Stream.concat(firstPage.playlists().stream(), secondPage.playlists().stream())
                .map(Playlist::getId)
                .toList();

        assertThat(firstPage.playlists()).hasSize(2);
        assertThat(secondPage.playlists()).hasSize(1);
        assertThat(allIds)
                .containsExactlyInAnyOrder(first.getId(), second.getId(), third.getId())
                .doesNotHaveDuplicates();
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(firstPage.playlists()).extracting(Playlist::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(secondPage.playlists()).extracting(Playlist::getId)
                .containsExactly(first.getId());
    }

    @Test
    void 생성시각이_같아도_id_타이브레이크로_정렬이_안정적이다() {
        UUID owner = persistUser("owner5@test.com");
        Instant sameInstant = Instant.parse("2026-09-14T00:00:00Z");

        Playlist first = persistPlaylist(owner, "플리1", "설명", GeneratedBy.USER, 0, sameInstant, sameInstant);
        Playlist second = persistPlaylist(owner, "플리2", "설명", GeneratedBy.USER, 0, sameInstant, sameInstant);
        Playlist third = persistPlaylist(owner, "플리3", "설명", GeneratedBy.USER, 0, sameInstant, sameInstant);
        flushAndClear();

        PlaylistQueryPage firstPage = playlistRepository.findAllByCondition(
                condition(owner, null, null, null, null, 2, SortType.CREATED_AT, SortDirection.DESCENDING));
        PlaylistQueryPage secondPage = playlistRepository.findAllByCondition(
                condition(owner, null, null, firstPage.nextCursor(), firstPage.nextIdAfter(), 2, SortType.CREATED_AT,
                        SortDirection.DESCENDING));

        List<UUID> allIds = Stream.concat(firstPage.playlists().stream(), secondPage.playlists().stream())
                .map(Playlist::getId)
                .toList();

        assertThat(allIds).containsExactly(third.getId(), second.getId(), first.getId());
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void SUBSCRIBER_COUNT_기준_정렬과_페이지네이션이_동작한다() {
        UUID owner = persistUser("owner6@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Playlist low = persistPlaylist(owner, "플리 low", "설명", GeneratedBy.USER, 1, t, t);
        Playlist mid = persistPlaylist(owner, "플리 mid", "설명", GeneratedBy.USER, 5, t.plusSeconds(1), t.plusSeconds(1));
        Playlist high = persistPlaylist(owner, "플리 high", "설명", GeneratedBy.USER, 10, t.plusSeconds(2),
                t.plusSeconds(2));
        flushAndClear();

        PlaylistQueryPage firstPage = playlistRepository.findAllByCondition(
                condition(owner, null, null, null, null, 2, SortType.SUBSCRIBER_COUNT, SortDirection.DESCENDING));
        PlaylistQueryPage secondPage = playlistRepository.findAllByCondition(
                condition(owner, null, null, firstPage.nextCursor(), firstPage.nextIdAfter(), 2,
                        SortType.SUBSCRIBER_COUNT, SortDirection.DESCENDING));

        assertThat(firstPage.playlists()).extracting(Playlist::getId)
                .containsExactly(high.getId(), mid.getId());
        assertThat(secondPage.playlists()).extracting(Playlist::getId)
                .containsExactly(low.getId());
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void 오름차순_정렬도_정상적으로_동작한다() {
        UUID owner = persistUser("owner7@test.com");
        Instant t = Instant.parse("2026-09-14T00:00:00Z");

        Playlist first = persistPlaylist(owner, "플리1", "설명", GeneratedBy.USER, 0, t, t);
        Playlist second = persistPlaylist(owner, "플리2", "설명", GeneratedBy.USER, 0, t.plusSeconds(1), t.plusSeconds(1));
        flushAndClear();

        PlaylistQueryPage result = playlistRepository.findAllByCondition(
                condition(owner, null, null, null, null, 20, SortType.CREATED_AT, SortDirection.ASCENDING));

        assertThat(result.playlists()).extracting(Playlist::getId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void CREATED_AT_커서가_잘못된_형식이면_DateTimeParseException이_발생한다() {
        UUID owner = UUID.randomUUID();

        assertThatThrownBy(() -> playlistRepository.findAllByCondition(
                condition(owner, null, null, "not-a-valid-instant", UUID.randomUUID(), 20, SortType.CREATED_AT,
                        SortDirection.DESCENDING)))
                .isInstanceOf(DateTimeParseException.class);
    }

    @Test
    void SUBSCRIBER_COUNT_커서가_잘못된_형식이면_NumberFormatException이_발생한다() {
        UUID owner = UUID.randomUUID();

        assertThatThrownBy(() -> playlistRepository.findAllByCondition(
                condition(owner, null, null, "not-a-number", UUID.randomUUID(), 20, SortType.SUBSCRIBER_COUNT,
                        SortDirection.DESCENDING)))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(NumberFormatException.class);
    }

    private Playlist persistPlaylist(
            UUID ownerId, String title, String description, GeneratedBy generatedBy, long subscriberCount,
            Instant createdAt, Instant updatedAt
    ) {
        Playlist playlist = Playlist.builder()
                .ownerId(ownerId)
                .title(title)
                .description(description)
                .generatedBy(generatedBy)
                .subscriberCount(subscriberCount)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
        entityManager.persist(playlist);
        return playlist;
    }

    private void persistSubscription(UUID playlistId, UUID subscriberId, Instant createdAt) {
        PlaylistSubscription subscription = PlaylistSubscription.builder()
                .id(new PlaylistSubscriptionId(playlistId, subscriberId))
                .createdAt(createdAt)
                .build();
        entityManager.persist(subscription);
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

    private PlaylistListCondition condition(
            UUID ownerId, UUID subscriberId, String keyword, String cursor, UUID idAfter, int limit,
            SortType sortBy, SortDirection sortDirection
    ) {
        return new PlaylistListCondition(ownerId, subscriberId, keyword, cursor, idAfter, limit, sortBy,
                sortDirection);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}