package com.codeit.modoo_playlist.moduleapi.domain.message.repository;

import com.codeit.modoo_playlist.core.domain.conversation.entity.SortDirection;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.message.entity.QMessage;
import com.codeit.modoo_playlist.core.domain.user.entity.QUser;
import com.codeit.modoo_playlist.moduleapi.dto.MessageDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.response.CursorResponseMessageDto;
import com.codeit.modoo_playlist.moduleapi.mapper.MessageMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final MessageMapper messageMapper;

    private static final QMessage m = new QMessage("message");
    private static final QUser sender = new QUser("sender");
    private static final QUser receiver = new QUser("receiver");

    @Override
    public CursorResponseMessageDto findMessages(
            UUID conversationId,
            SliceCursorRequest request
    ) {
        boolean ascending = isAscending(request);
        int limit = request.limit();

        List<Message> messages = queryFactory
                .selectFrom(m)
                .leftJoin(m.sender, sender).fetchJoin()
                .leftJoin(m.receiver, receiver).fetchJoin()
                .where(
                        m.conversation.id.eq(conversationId),
                        cursorCondition(request, ascending)
                )
                .orderBy(
                        ascending ? m.createdAt.asc() : m.createdAt.desc(),
                        ascending ? m.id.asc() : m.id.desc()
                )
                .limit(limit + 1L)
                .fetch();

        boolean hasNext = messages.size() > limit;

        if (hasNext) {
            messages = messages.subList(0, limit);
        }

        List<MessageDto> data = messages.stream()
                .map(messageMapper::toDto)
                .toList();

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !messages.isEmpty()) {
            Message lastMessage = messages.get(messages.size() - 1);

            nextCursor = lastMessage.getCreatedAt().toString();
            nextIdAfter = lastMessage.getId();
        }

        Long count = queryFactory
                .select(m.count())
                .from(m)
                .where(m.conversation.id.eq(conversationId))
                .fetchOne();

        long totalCount = count != null ? count : 0L;

        return new CursorResponseMessageDto(
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
    public Optional<Message> findMessageForRead(
            UUID messageId,
            UUID conversationId,
            UUID receiverId
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(m)
                        .where(
                                m.id.eq(messageId),
                                m.conversation.id.eq(conversationId),
                                m.receiver.id.eq(receiverId)
                        )
                        .fetchOne());
    }

    private BooleanExpression cursorCondition(
            SliceCursorRequest request,
            boolean ascending
    ) {
        if (request.cursor() == null
                || request.cursor().isBlank()) {
            return null;
        }

        Instant cursor = Instant.parse(request.cursor());

        BooleanExpression createdAtCondition = ascending
                ? m.createdAt.gt(cursor)
                : m.createdAt.lt(cursor);

        // 보조 커서 입력 확인
        if (request.idAfter() == null) {
            return createdAtCondition;
        }

        BooleanExpression sameCreatedAtCondition = m.createdAt.eq(cursor).and(
                ascending
                        ? m.id.gt(request.idAfter())
                        : m.id.lt(request.idAfter())
        );

        return createdAtCondition.or(sameCreatedAtCondition);
    }

    private boolean isAscending(SliceCursorRequest request) {
        if (request.limit() <= 0) {
            throw new IllegalArgumentException("limit은 0 보다 커야합니다.");
        }
        if (request.sortBy() != null
                && !request.sortBy().isBlank()
                && !"createdAt".equals(request.sortBy())) {
            throw new IllegalArgumentException(
                    "createdAt 정렬만 지원합니다."
            );
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

        throw new IllegalArgumentException("지원하지 않는 정렬방향입니다.: " + direction);
    }
}
