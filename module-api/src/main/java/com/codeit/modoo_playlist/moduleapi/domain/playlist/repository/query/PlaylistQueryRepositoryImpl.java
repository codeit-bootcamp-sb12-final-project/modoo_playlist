package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query;

import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import com.codeit.modoo_playlist.core.domain.playlist.entity.QPlaylist;
import com.codeit.modoo_playlist.core.domain.playlist.entity.QPlaylistSubscription;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PlaylistQueryRepositoryImpl implements PlaylistQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QPlaylist playlist = QPlaylist.playlist;
    private static final QPlaylistSubscription playlistSubscription = QPlaylistSubscription.playlistSubscription;

    @Override
    public PlaylistQueryPage findAllByCondition(PlaylistListCondition condition) {
        BooleanBuilder filter = createFilter(condition);

        List<Playlist> results = queryFactory
                .selectFrom(playlist)
                .where(filter, applyCursor(condition))
                .orderBy(orderSpecifiers(condition))
                .limit(condition.limit() + 1L)
                .fetch();

        boolean hasNext = results.size() > condition.limit();
        List<Playlist> content = hasNext ? results.subList(0, condition.limit()) : results;

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !content.isEmpty()) {
            Playlist last = content.get(content.size() - 1);
            nextCursor = cursorValue(last, condition.sortBy());
            nextIdAfter = last.getId();
        }

        long totalCount = queryFactory
                .select(playlist.count())
                .from(playlist)
                .where(filter)
                .fetchOne();

        return new PlaylistQueryPage(content, nextCursor, nextIdAfter, hasNext, totalCount);
    }

    private BooleanBuilder createFilter(PlaylistListCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.ownerId() != null) {
            builder.and(playlist.ownerId.eq(condition.ownerId()));
        }

        if (condition.keyword() != null) {
            builder.and(playlist.title.containsIgnoreCase(condition.keyword()));
        }

        if (condition.subscriberId() != null) {
            builder.and(JPAExpressions.selectOne()
                    .from(playlistSubscription)
                    .where(
                            playlistSubscription.id.playlistId.eq(playlist.id),
                            playlistSubscription.id.subscriberId.eq(condition.subscriberId())
                    )
                    .exists());
        }

        return builder;
    }

    private BooleanBuilder applyCursor(PlaylistListCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (condition.cursor() == null || condition.idAfter() == null) {
            return builder;
        }

        Instant cursorInstant = Instant.parse(condition.cursor());
        boolean desc = condition.sortDirection() == PlaylistListCondition.SortDirection.DESCENDING;

        if (condition.sortBy() == PlaylistListCondition.SortType.CREATED_AT) {
            builder.and(desc
                    ? playlist.createdAt.lt(cursorInstant)
                    .or(playlist.createdAt.eq(cursorInstant).and(playlist.id.lt(condition.idAfter())))
                    : playlist.createdAt.gt(cursorInstant)
                    .or(playlist.createdAt.eq(cursorInstant).and(playlist.id.gt(condition.idAfter()))));
        } else {
            builder.and(desc
                    ? playlist.updatedAt.lt(cursorInstant)
                    .or(playlist.updatedAt.eq(cursorInstant).and(playlist.id.lt(condition.idAfter())))
                    : playlist.updatedAt.gt(cursorInstant)
                    .or(playlist.updatedAt.eq(cursorInstant).and(playlist.id.gt(condition.idAfter()))));
        }

        return builder;
    }

    private OrderSpecifier<?>[] orderSpecifiers(PlaylistListCondition condition) {
        boolean desc = condition.sortDirection() == PlaylistListCondition.SortDirection.DESCENDING;

        if (condition.sortBy() == PlaylistListCondition.SortType.CREATED_AT) {
            return new OrderSpecifier<?>[]{
                    desc ? playlist.createdAt.desc() : playlist.createdAt.asc(),
                    desc ? playlist.id.desc() : playlist.id.asc()
            };
        }
        return new OrderSpecifier<?>[]{
                desc ? playlist.updatedAt.desc() : playlist.updatedAt.asc(),
                desc ? playlist.id.desc() : playlist.id.asc()
        };
    }

    private String cursorValue(Playlist p, PlaylistListCondition.SortType sortBy) {
        return sortBy == PlaylistListCondition.SortType.CREATED_AT
                ? p.getCreatedAt().toString()
                : p.getUpdatedAt().toString();
    }
}