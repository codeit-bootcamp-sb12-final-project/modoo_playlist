package com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query;

import com.codeit.modoo_playlist.core.domain.notification.entity.Notification;
import com.codeit.modoo_playlist.core.domain.notification.entity.QNotification;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepositoryImpl implements NotificationQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QNotification notification = QNotification.notification;

    @Override
    public NotificationQueryPage findAllByCondition(NotificationListCondition condition) {
        boolean desc = condition.sortDirection() == NotificationListCondition.SortDirection.DESCENDING;

        List<Notification> results = queryFactory
                .selectFrom(notification)
                .where(
                        notification.receiverId.eq(condition.receiverId()),
                        applyCursor(condition, desc)
                )
                .orderBy(orderSpecifiers(desc))
                .limit(condition.limit() + 1L)
                .fetch();

        boolean hasNext = results.size() > condition.limit();
        List<Notification> content = hasNext ? results.subList(0, condition.limit()) : results;

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !content.isEmpty()) {
            Notification last = content.get(content.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

        long totalCount = queryFactory
                .select(notification.count())
                .from(notification)
                .where(notification.receiverId.eq(condition.receiverId()))
                .fetchOne();

        return new NotificationQueryPage(content, nextCursor, nextIdAfter, hasNext, totalCount);
    }

    private BooleanBuilder applyCursor(NotificationListCondition condition, boolean desc) {
        BooleanBuilder builder = new BooleanBuilder();
        if (condition.cursor() == null || condition.idAfter() == null) {
            return builder;
        }

        Instant cursorInstant = Instant.parse(condition.cursor());
        builder.and(desc
                ? notification.createdAt.lt(cursorInstant)
                .or(notification.createdAt.eq(cursorInstant).and(notification.id.lt(condition.idAfter())))
                : notification.createdAt.gt(cursorInstant)
                .or(notification.createdAt.eq(cursorInstant).and(notification.id.gt(condition.idAfter()))));

        return builder;
    }

    private OrderSpecifier<?>[] orderSpecifiers(boolean desc) {
        return new OrderSpecifier<?>[]{
                desc ? notification.createdAt.desc() : notification.createdAt.asc(),
                desc ? notification.id.desc() : notification.id.asc()
        };
    }
}
