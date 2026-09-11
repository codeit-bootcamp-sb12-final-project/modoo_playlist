package com.codeit.modoo_playlist.moduleapi.domain.review.repository.query;

import com.codeit.modoo_playlist.core.domain.review.entity.QReview;
import com.codeit.modoo_playlist.core.domain.review.entity.Review;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReviewQueryRepositoryImpl implements ReviewQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QReview review = QReview.review;

    @Override
    public ReviewQueryPage findAllByCondition(ReviewListCondition condition) {
        BooleanBuilder filter = createFilter(condition);

        List<Review> results = queryFactory
                .selectFrom(review)
                .where(filter, applyCursor(condition))
                .orderBy(orderSpecifiers(condition))
                .limit(condition.limit() + 1L)
                .fetch();

        boolean hasNext = results.size() > condition.limit();
        List<Review> content = hasNext ? results.subList(0, condition.limit()) : results;

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !content.isEmpty()) {
            Review last = content.get(content.size() - 1);
            nextCursor = cursorValue(last);
            nextIdAfter = last.getId();
        }

        long totalCount = queryFactory
                .select(review.count())
                .from(review)
                .where(filter)
                .fetchOne();

        return new ReviewQueryPage(content, nextCursor, nextIdAfter, hasNext, totalCount);
    }

    private BooleanBuilder createFilter(ReviewListCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.contentId() != null) {
            builder.and(review.contentId.eq(condition.contentId()));
        }

        return builder;
    }

    private BooleanBuilder applyCursor(ReviewListCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();
        if (condition.cursor() == null || condition.idAfter() == null) {
            return builder;
        }

        Instant cursorInstant = Instant.parse(condition.cursor());
        boolean desc = condition.sortDirection() == ReviewListCondition.SortDirection.DESCENDING;

        builder.and(desc
                ? review.createdAt.lt(cursorInstant)
                .or(review.createdAt.eq(cursorInstant).and(review.id.lt(condition.idAfter())))
                : review.createdAt.gt(cursorInstant)
                .or(review.createdAt.eq(cursorInstant).and(review.id.gt(condition.idAfter()))));

        return builder;
    }

    private OrderSpecifier<?>[] orderSpecifiers(ReviewListCondition condition) {
        boolean desc = condition.sortDirection() == ReviewListCondition.SortDirection.DESCENDING;

        return new OrderSpecifier<?>[]{
                desc ? review.createdAt.desc() : review.createdAt.asc(),
                desc ? review.id.desc() : review.id.asc()
        };
    }

    private String cursorValue(Review r) {
        return r.getCreatedAt().toString();
    }
}