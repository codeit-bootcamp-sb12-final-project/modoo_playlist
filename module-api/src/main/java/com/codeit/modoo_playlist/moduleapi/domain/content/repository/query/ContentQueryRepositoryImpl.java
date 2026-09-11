package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import static com.codeit.modoo_playlist.core.domain.content.entity.QContent.content;
import static com.codeit.modoo_playlist.core.domain.content.entity.QContentTag.contentTag;
import static com.codeit.modoo_playlist.core.domain.tag.entity.QTag.tag;
import static com.codeit.modoo_playlist.core.domain.watchingSession.entity.QWatchingSession.watchingSession;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
            filter.and(JPAExpressions
                    .selectOne()
                    .from(contentTag)
                    .join(contentTag.tag, tag)
                    .where(
                            contentTag.content.eq(content),
                            tag.name.in(condition.tagNames())
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
        };

        OrderSpecifier<?> tieBreaker = new OrderSpecifier<>(order, content.id);
        return new OrderSpecifier<?>[]{primary, tieBreaker};
    }

    private String cursorValue(ContentItem result, SortType sortBy) {
        return switch (sortBy) {
            case WATCHER_COUNT -> Long.toString(result.watcherCount());
            case CREATED_AT -> result.content().getCreatedAt().toString();
            case AVERAGE_RATING -> result.content().getAverageRating().toPlainString();
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
}
