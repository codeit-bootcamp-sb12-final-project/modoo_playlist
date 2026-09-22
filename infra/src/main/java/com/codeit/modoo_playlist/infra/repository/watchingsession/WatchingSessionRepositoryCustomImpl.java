package com.codeit.modoo_playlist.infra.repository.watchingsession;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.conversation.entity.SortDirection;
import com.codeit.modoo_playlist.core.domain.watchingSession.dto.WatchingSessionDto;
import com.codeit.modoo_playlist.core.domain.watchingSession.entity.QWatchingSession;
import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.core.global.common.dto.base.SliceCursorRequest;
import com.codeit.modoo_playlist.infra.mapper.ContentSummaryMapper;
import com.codeit.modoo_playlist.core.domain.content.dto.ContentSummaryResponse;
import com.codeit.modoo_playlist.infra.repository.RealtimeContentTagRepository;
import com.codeit.modoo_playlist.core.domain.watchingSession.dto.CursorResponseWatchingSessionDto;
import com.codeit.modoo_playlist.infra.mapper.WatchingSessionMapper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class WatchingSessionRepositoryCustomImpl implements WatchingSessionRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final WatchingSessionMapper watchingSessionMapper;
    private final ContentSummaryMapper contentSummaryMapper;

    private final RealtimeContentTagRepository contentTagRepository;

    private static final QWatchingSession ws = QWatchingSession.watchingSession;

    @Override
    public Optional<WatchingSession> findActiveByWatcherId(UUID watcherId) {
        WatchingSession session = queryFactory
                .selectFrom(ws)
                .join(ws.watcher).fetchJoin()
                .join(ws.content).fetchJoin()
                .where(
                        ws.watcher.id.eq(watcherId),
                        ws.endedAt.isNull()
                )
                .orderBy(
                        ws.createdAt.desc(),
                        ws.id.desc()
                )
                .fetchFirst();

        return Optional.ofNullable(session);
    }

    @Override
    public CursorResponseWatchingSessionDto findActiveByContent(
            UUID contentId,
            String watcherNameLike,
            SliceCursorRequest request
    ) {
        boolean ascending = isAscending(request);
        int limit = request.limit();

        List<WatchingSession> rows = queryFactory
                .selectFrom(ws)
                .join(ws.watcher).fetchJoin()
                .join(ws.content).fetchJoin()
                .where(
                        filter(contentId, watcherNameLike),
                        cursorCondition(request, ascending)
                )
                .orderBy(
                        ascending ? ws.createdAt.asc() : ws.createdAt.desc(),
                        ascending ? ws.id.asc() : ws.id.desc()
                )
                .limit(limit + 1L)
                .fetch();

        boolean hasNext = rows.size() > limit;

        if (hasNext) {
            rows = rows.subList(0, limit);
        }



        List<WatchingSessionDto> data = List.of();

        if (!rows.isEmpty()) {
            Content content = rows.get(0).getContent();

            List<String> tags = contentTagRepository
                    .findAllWithTagByContentIds(List.of(contentId))
                    .stream()
                    .map(contentTag -> contentTag.getTag().getName())
                    .toList();

            ContentSummaryResponse summary =
                    contentSummaryMapper.toSummary(content, tags);

            data = rows.stream()
                    .map(row -> watchingSessionMapper.toDto(row, summary))
                    .toList();
        }

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !rows.isEmpty()) {
            WatchingSession lastWatchingSession = rows.get(rows.size() - 1);

            nextCursor = lastWatchingSession.getCreatedAt().toString();
            nextIdAfter = lastWatchingSession.getId();
        }

        Long count = queryFactory
                .select(ws.id.countDistinct())
                .from(ws)
                .join(ws.watcher)
                .where(filter(contentId, watcherNameLike))
                .fetchOne();

        long totalCount = count != null ? count : 0L;

        return new CursorResponseWatchingSessionDto(
                data,
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                "createdAt",
                ascending ? SortDirection.ASCENDING : SortDirection.DESCENDING
        );
    }

    @Override
    public void touchActiveSessions(
            Collection<UUID> sessionIds,
            Instant touchedAt
    ) {
        if (sessionIds.isEmpty()) {
            return;
        }

        queryFactory
                .update(ws)
                .set(ws.updatedAt, touchedAt)
                .where(
                        ws.id.in(sessionIds),
                        ws.endedAt.isNull()
                )
                .execute();
    }

    private BooleanBuilder filter(
            UUID contentId,
            String watcherNameLike
    ) {
        BooleanBuilder builder = new BooleanBuilder()
                .and(ws.content.id.eq(contentId))
                .and(ws.endedAt.isNull());

        if (watcherNameLike != null && !watcherNameLike.isBlank()) {
            builder.and(
                    ws.watcher.username
                            .containsIgnoreCase(watcherNameLike.trim())
            );
        }

        return builder;
    }

    private BooleanExpression cursorCondition(
            SliceCursorRequest request,
            boolean ascending
    ) {
        if (request.cursor() == null || request.cursor().isBlank()) {
            return null;
        }

        Instant cursor = Instant.parse(request.cursor());

        BooleanExpression createdAtCondition = ascending
                ? ws.createdAt.gt(cursor)
                : ws.createdAt.lt(cursor);

        // 보조 커서 입력 확인
        if (request.idAfter() == null) {
            return createdAtCondition;
        }

        // 생성 시간이 같은 경우 보조 커서
        BooleanExpression sameCreatedAtCondition = ws.createdAt.eq(cursor)
                .and(ascending
                        ? ws.id.gt(request.idAfter())
                        : ws.id.lt(request.idAfter()));

        return createdAtCondition.or(sameCreatedAtCondition);
    }

    private boolean isAscending(SliceCursorRequest request) {
        if (request.limit() < 1) {
            throw new IllegalArgumentException("limit은 0보다 커야합니다.");
        }
        if (request.sortBy() != null
                && !request.sortBy().isBlank()
                && !"createdAt".equals(request.sortBy())) {
            throw new IllegalArgumentException("createdAt 정렬만 지원합니다.");
        }
        String direction = request.sortDirection();

        if (direction == null
                || direction.isBlank()
                || "DESC".equalsIgnoreCase(direction)
                || "DESCENDING".equalsIgnoreCase(direction)) {
            return false;
        }

        if ("ASC".equalsIgnoreCase(direction)
                || "ASCENDING".equalsIgnoreCase(direction)) {
            return true;
        }
        throw new IllegalArgumentException("지원하지 않는 정렬 방향입니다.: " + direction);
    }
}
