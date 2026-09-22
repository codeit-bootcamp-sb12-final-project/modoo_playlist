package com.codeit.modoo_playlist.moduleapi.domain.follow.repository.query;

import com.codeit.modoo_playlist.core.domain.follow.entity.Follow;
import com.codeit.modoo_playlist.core.domain.follow.entity.QFollow;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FollowQueryRepositoryImpl implements FollowQueryRepository {

    private static final QFollow follow = QFollow.follow;

    private final JPAQueryFactory queryFactory;

    @Override
    public FollowQueryPage findAllByCondition(FollowListCondition condition) {
        BooleanBuilder filter = createFilter(condition);

        List<Follow> results = queryFactory
                .selectFrom(follow)
                .where(filter, applyCursor(condition))
                .orderBy(orderSpecifiers(condition))
                .limit(condition.limit() + 1L)
                .fetch();

        boolean hasNext = results.size() > condition.limit();
        List<Follow> content = hasNext ? results.subList(0, condition.limit()) : results;

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !content.isEmpty()) {
            Follow last = content.get(content.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

        Long totalCount = queryFactory
                .select(follow.count())
                .from(follow)
                .where(filter)
                .fetchOne();

        return new FollowQueryPage(content, nextCursor, nextIdAfter, hasNext,
                totalCount == null ? 0 : totalCount);
    }

    private BooleanBuilder createFilter(FollowListCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.type() == FollowListType.FOLLOWERS) {
            builder.and(follow.followeeId.eq(condition.userId()));
        } else {
            builder.and(follow.followerId.eq(condition.userId()));
        }

        return builder;
    }

    private BooleanBuilder applyCursor(FollowListCondition condition) {
        BooleanBuilder builder = new BooleanBuilder();

        if (condition.cursor() == null || condition.idAfter() == null) {
            return builder;
        }

        Instant cursorInstant;
        try {
            cursorInstant = Instant.parse(condition.cursor());
        } catch (DateTimeParseException e) {
            throw new BaseException(ErrorCode.INVALID_REQUEST, e);
        }

        boolean desc = condition.sortDirection() == FollowListCondition.SortDirection.DESCENDING;

        builder.and(desc
                ? follow.createdAt.lt(cursorInstant)
                .or(follow.createdAt.eq(cursorInstant).and(follow.id.lt(condition.idAfter())))
                : follow.createdAt.gt(cursorInstant)
                .or(follow.createdAt.eq(cursorInstant).and(follow.id.gt(condition.idAfter()))));

        return builder;
    }

    private OrderSpecifier<?>[] orderSpecifiers(FollowListCondition condition) {
        boolean desc = condition.sortDirection() == FollowListCondition.SortDirection.DESCENDING;

        return new OrderSpecifier<?>[]{
                desc ? follow.createdAt.desc() : follow.createdAt.asc(),
                desc ? follow.id.desc() : follow.id.asc()
        };
    }
}