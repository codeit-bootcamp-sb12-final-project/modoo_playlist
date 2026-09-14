package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import static com.codeit.modoo_playlist.core.domain.content.entity.QContent.content;
import static com.codeit.modoo_playlist.core.domain.content.entity.QContentTag.contentTag;
import static com.codeit.modoo_playlist.core.domain.preference.entity.QUserPreferenceTag.userPreferenceTag;
import static com.codeit.modoo_playlist.core.domain.watchingSession.entity.QWatchingSession.watchingSession;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import com.codeit.modoo_playlist.core.domain.content.entity.QContentTag;
import com.codeit.modoo_playlist.core.domain.tag.entity.QTag;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentListCondition.SortDirection;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentListCondition.SortType;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentQueryPage.ContentItem;

import jakarta.persistence.EntityManager;

public class ContentQueryRepositoryImpl implements ContentQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ContentQueryRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public ContentQueryPage findAllByCondition(ContentListCondition condition) {
        return findAllByCondition(condition, null);
    }

    @Override
    public ContentQueryPage findAllByCondition(ContentListCondition condition, UUID userId) {
        if (condition.sortBy() == SortType.RECOMMENDED) {
            if (!hasPreferenceScore(userId)) {
                return findAllByStandardSort(withSort(condition, SortType.WATCHER_COUNT));
            }
            return findAllByRecommendation(condition, userId);
        }
        return findAllByStandardSort(condition);
    }

    private ContentQueryPage findAllByStandardSort(ContentListCondition condition) {
        BooleanBuilder filter = createFilter(condition);
        NumberExpression<Long> watcherCount = watchingSession.watcher.id.countDistinct();

        JPAQuery<Tuple> query = queryFactory
                .select(content, watcherCount)
                .from(content)
                .leftJoin(watchingSession)
                .on(
                        watchingSession.content.eq(content),
                        watchingSession.endedAt.isNull()
                )
                .where(filter)
                .groupBy(content.id);

        applyCursor(query, condition, watcherCount);

        List<Tuple> tuples = query
                .orderBy(orderSpecifiers(condition, watcherCount))
                .limit(condition.limit() + 1L)
                .fetch();

        boolean hasNext = tuples.size() > condition.limit();
        if (hasNext) {
            tuples = new ArrayList<>(tuples.subList(0, condition.limit()));
        }

        List<ContentItem> results = tuples.stream()
                .map(tuple -> new ContentItem(
                        tuple.get(content),
                        valueOrZero(tuple.get(watcherCount))
                ))
                .toList();

        Long totalCount = queryFactory
                .select(content.count())
                .from(content)
                .where(createFilter(condition))
                .fetchOne();

        ContentItem last = hasNext ? results.get(results.size() - 1) : null;

        return new ContentQueryPage(
                results,
                last == null ? null : cursorValue(last, condition.sortBy()),
                last == null ? null : last.content().getId(),
                hasNext,
                valueOrZero(totalCount)
        );
    }

    private ContentQueryPage findAllByRecommendation(ContentListCondition condition, UUID userId) {
        BooleanBuilder filter = createFilter(condition);
        NumberExpression<BigDecimal> recommendationScore = userPreferenceTag.score.sumAggregate()
                .coalesce(BigDecimal.ZERO);

        JPAQuery<Tuple> query = queryFactory
                .select(content, recommendationScore)
                .from(content)
                .leftJoin(contentTag).on(contentTag.content.eq(content))
                .leftJoin(userPreferenceTag).on(
                        userPreferenceTag.id.userId.eq(userId),
                        userPreferenceTag.id.tagId.eq(contentTag.id.tagId)
                )
                .where(filter)
                .groupBy(content.id);

        applyRecommendationCursor(query, condition, recommendationScore);

        Order order = condition.sortDirection() == SortDirection.ASCENDING
                ? Order.ASC
                : Order.DESC;
        List<Tuple> tuples = query
                .orderBy(
                        new OrderSpecifier<>(order, recommendationScore),
                        new OrderSpecifier<>(order, content.id)
                )
                .limit(condition.limit() + 1L)
                .fetch();

        boolean hasNext = tuples.size() > condition.limit();
        if (hasNext) {
            tuples = new ArrayList<>(tuples.subList(0, condition.limit()));
        }

        List<UUID> contentIds = tuples.stream()
                .map(tuple -> tuple.get(content).getId())
                .toList();
        Map<UUID, Long> watcherCounts = loadWatcherCounts(contentIds);
        List<ContentItem> results = tuples.stream()
                .map(tuple -> {
                    var itemContent = tuple.get(content);
                    return new ContentItem(
                            itemContent,
                            watcherCounts.getOrDefault(itemContent.getId(), 0L)
                    );
                })
                .toList();

        Long totalCount = queryFactory
                .select(content.count())
                .from(content)
                .where(createFilter(condition))
                .fetchOne();
        Tuple last = hasNext ? tuples.get(tuples.size() - 1) : null;

        return new ContentQueryPage(
                results,
                last == null ? null : valueOrZero(last.get(recommendationScore)).toPlainString(),
                last == null ? null : last.get(content).getId(),
                hasNext,
                valueOrZero(totalCount)
        );
    }

    private boolean hasPreferenceScore(UUID userId) {
        if (userId == null) {
            return false;
        }
        return queryFactory
                .selectOne()
                .from(userPreferenceTag)
                .where(
                        userPreferenceTag.id.userId.eq(userId),
                        userPreferenceTag.score.ne(BigDecimal.ZERO)
                )
                .fetchFirst() != null;
    }

    private Map<UUID, Long> loadWatcherCounts(List<UUID> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = new HashMap<>();
        NumberExpression<Long> watcherCount = watchingSession.watcher.id.countDistinct();
        queryFactory
                .select(watchingSession.content.id, watcherCount)
                .from(watchingSession)
                .where(
                        watchingSession.content.id.in(contentIds),
                        watchingSession.endedAt.isNull()
                )
                .groupBy(watchingSession.content.id)
                .fetch()
                .forEach(tuple -> counts.put(
                        tuple.get(watchingSession.content.id),
                        valueOrZero(tuple.get(watcherCount))
                ));
        return counts;
    }

    private void applyRecommendationCursor(
            JPAQuery<Tuple> query,
            ContentListCondition condition,
            NumberExpression<BigDecimal> recommendationScore
    ) {
        if (condition.cursor() == null) {
            return;
        }
        BigDecimal cursor = parseBigDecimal(condition.cursor());
        boolean ascending = condition.sortDirection() == SortDirection.ASCENDING;
        BooleanExpression idAfter = ascending
                ? content.id.gt(condition.idAfter())
                : content.id.lt(condition.idAfter());
        BooleanExpression primary = ascending
                ? recommendationScore.gt(cursor)
                : recommendationScore.lt(cursor);
        query.having(primary.or(recommendationScore.eq(cursor).and(idAfter)));
    }

    private ContentListCondition withSort(ContentListCondition condition, SortType sortType) {
        return new ContentListCondition(
                condition.type(), condition.keyword(), condition.tagNames(),
                condition.cursor(), condition.idAfter(), condition.limit(),
                sortType, condition.sortDirection()
        );
    }

    @Override
    public long countCurrentWatchers(UUID contentId) {
        Long watcherCount = queryFactory
                .select(watchingSession.watcher.id.countDistinct())
                .from(watchingSession)
                .where(
                        watchingSession.content.id.eq(contentId),
                        watchingSession.endedAt.isNull()
                )
                .fetchOne();

        return valueOrZero(watcherCount);
    }

    private BooleanBuilder createFilter(ContentListCondition condition) {
        BooleanBuilder filter = new BooleanBuilder(content.deletedAt.isNull());

        if (condition.type() != null) {
            filter.and(content.type.eq(condition.type()));
        }
        if (condition.keyword() != null) {
            filter.and(content.title.containsIgnoreCase(condition.keyword()));
        }
        if (!condition.tagNames().isEmpty()) {
            QContentTag filterContentTag = new QContentTag("filterContentTag");
            QTag filterTag = new QTag("filterTag");
            filter.and(JPAExpressions
                    .selectOne()
                    .from(filterContentTag)
                    .join(filterContentTag.tag, filterTag)
                    .where(
                            filterContentTag.content.eq(content),
                            filterTag.name.in(condition.tagNames())
                    )
                    .exists());
        }

        return filter;
    }

    private void applyCursor(
            JPAQuery<Tuple> query,
            ContentListCondition condition,
            NumberExpression<Long> watcherCount
    ) {
        if (condition.cursor() == null) {
            return;
        }

        boolean ascending = condition.sortDirection() == SortDirection.ASCENDING;
        BooleanExpression idAfter = ascending
                ? content.id.gt(condition.idAfter())
                : content.id.lt(condition.idAfter());

        switch (condition.sortBy()) {
            case CREATED_AT -> {
                Instant cursor = parseInstant(condition.cursor());
                BooleanExpression primary = ascending
                        ? content.createdAt.gt(cursor)
                        : content.createdAt.lt(cursor);
                query.where(primary.or(content.createdAt.eq(cursor).and(idAfter)));
            }
            case AVERAGE_RATING -> {
                BigDecimal cursor = parseBigDecimal(condition.cursor());
                BooleanExpression primary = ascending
                        ? content.averageRating.gt(cursor)
                        : content.averageRating.lt(cursor);
                query.where(primary.or(content.averageRating.eq(cursor).and(idAfter)));
            }
            case WATCHER_COUNT -> {
                long cursor = parseLong(condition.cursor());
                BooleanExpression primary = ascending
                        ? watcherCount.gt(cursor)
                        : watcherCount.lt(cursor);
                query.having(primary.or(watcherCount.eq(cursor).and(idAfter)));
            }
            case RECOMMENDED -> throw new IllegalStateException("추천 정렬은 전용 조회에서 처리합니다.");
        }
    }

    private OrderSpecifier<?>[] orderSpecifiers(
            ContentListCondition condition,
            NumberExpression<Long> watcherCount
    ) {
        Order order = condition.sortDirection() == SortDirection.ASCENDING
                ? Order.ASC
                : Order.DESC;

        OrderSpecifier<?> primary = switch (condition.sortBy()) {
            case WATCHER_COUNT -> new OrderSpecifier<>(order, watcherCount);
            case CREATED_AT -> new OrderSpecifier<>(order, content.createdAt);
            case AVERAGE_RATING -> new OrderSpecifier<>(order, content.averageRating);
            case RECOMMENDED -> throw new IllegalStateException("추천 정렬은 전용 조회에서 처리합니다.");
        };

        OrderSpecifier<?> tieBreaker = new OrderSpecifier<>(order, content.id);
        return new OrderSpecifier<?>[]{primary, tieBreaker};
    }

    private String cursorValue(ContentItem result, SortType sortBy) {
        return switch (sortBy) {
            case WATCHER_COUNT -> Long.toString(result.watcherCount());
            case CREATED_AT -> result.content().getCreatedAt().toString();
            case AVERAGE_RATING -> result.content().getAverageRating().toPlainString();
            case RECOMMENDED -> throw new IllegalStateException("추천 정렬은 전용 조회에서 처리합니다.");
        };
    }

    private Instant parseInstant(String cursor) {
        try {
            return Instant.parse(cursor);
        } catch (RuntimeException exception) {
            throw invalidCursor("createdAt", cursor, exception);
        }
    }

    private BigDecimal parseBigDecimal(String cursor) {
        try {
            return new BigDecimal(cursor);
        } catch (NumberFormatException exception) {
            throw invalidCursor("averageRating", cursor, exception);
        }
    }

    private long parseLong(String cursor) {
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException exception) {
            throw invalidCursor("watcherCount", cursor, exception);
        }
    }

    private BaseException invalidCursor(String sortBy, String cursor, RuntimeException cause) {
        BaseException exception = new BaseException(ErrorCode.CONTENT_CURSOR_INVALID, cause);
        exception.addDetail("sortBy", sortBy);
        exception.addDetail("cursor", cursor);
        return exception;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
