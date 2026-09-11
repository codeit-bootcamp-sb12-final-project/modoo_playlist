package com.codeit.modoo_playlist.moduleapi.domain.conversation.repository;

import com.codeit.modoo_playlist.core.domain.conversation.entity.*;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.message.entity.QMessage;
import com.codeit.modoo_playlist.core.domain.user.entity.QUser;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.dto.ConversationDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.response.CursorResponseConversationDto;
import com.codeit.modoo_playlist.moduleapi.mapper.ConversationMapper;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.swing.*;
import java.time.Instant;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class ConversationRepositoryImpl implements ConversationRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final ConversationMapper conversationMapper;

    private static final QConversation c = QConversation.conversation;

    private static final QConversationParticipant cp =
            QConversationParticipant.conversationParticipant;

    // 같은 참여자 테이블에 요청자와 상대방으로 각각 조인하기 위해 분리
    private static final QConversationParticipant me = new QConversationParticipant("me");
    private static final QConversationParticipant other = new QConversationParticipant("other");

    private static final QUser u = QUser.user;
    private static final QMessage m = new QMessage("message");
    private static final QUser sender = new QUser("sender");
    private static final QUser receiver = new QUser("receiver");

    @Override
    public CursorResponseConversationDto findConversations(
            UUID requesterId,
            String keywordLike,
            SliceCursorRequest request
    ) {
        boolean ascending = isAscending(request);
        int limit = request.limit();

        List<Tuple> rows = queryFactory
                .select(c, u)
                .from(c)
                .join(c.participants, me)
                .join(c.participants, other)
                .join(other.user, u)
                .where(
                        c.type.eq(ConversationType.DM),
                        me.user.id.eq(requesterId),
                        other.user.id.ne(requesterId),
                        keywordCondition(keywordLike),
                        cursorCondition(request, ascending)
                )
                .orderBy(
                        ascending ? c.createdAt.asc() : c.createdAt.desc(),
                        ascending ? c.id.asc() : c.id.desc()
                )
                .limit(limit + 1L)
                .fetch();

        boolean hasNext = rows.size() > limit;

        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        List<UUID> conversationIds = rows.stream()
                .map(row -> row.get(c).getId())
                .toList();

        Map<UUID, Message> lastestMessage =
                findLastestMessage(conversationIds);

        Set<UUID> unreadConversationIds =
                findUnreadConversationIds(conversationIds, requesterId);

        List<ConversationDto> data = rows.stream()
                .map(row -> {
                    Conversation conversation = row.get(c);
                    User withUser = row.get(u);
                    UUID conversationId = conversation.getId();

                    return conversationMapper.toDto(
                            conversation,
                            withUser,
                            lastestMessage.get(conversationId),
                            unreadConversationIds.contains(conversationId)
                    );
                })
                .toList();

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !rows.isEmpty()) {
            Conversation lastConversation = rows.get(rows.size() - 1).get(c);

            nextCursor = lastConversation.getCreatedAt().toString();
            nextIdAfter = lastConversation.getId();
        }

        Long count = queryFactory
                .select(c.id.countDistinct())
                .from(c)
                .join(c.participants, me)
                .join(c.participants, other)
                .join(other.user, u)
                .where(
                        c.type.eq(ConversationType.DM),
                        me.user.id.eq(requesterId),
                        other.user.id.ne(requesterId),
                        keywordCondition(keywordLike)
                )
                .fetchOne();

        long totalCount = count != null ? count : 0L;

        return new CursorResponseConversationDto(
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
    public Optional<ConversationDto> findConversation(
            UUID requesterId,
            UUID conversationId
    ) {

        Tuple result = queryFactory
                .select(c, u)
                .from(c)
                .join(c.participants, me)
                .join(c.participants, other)
                .join(other.user, u)
                .where(
                        c.type.eq(ConversationType.DM),
                        c.id.eq(conversationId),
                        me.user.id.eq(requesterId),
                        other.user.id.ne(requesterId)
                )
                .fetchOne();

        return Optional.ofNullable(result)
                .map(row -> toDto(row, requesterId));
    }

    @Override
    public Optional<ConversationDto> findDmConversation(
            UUID requesterId,
            UUID withUserId
    ) {
        Tuple result = queryFactory
                .select(c, u)
                .from(c)
                .join(c.participants, me)
                .join(c.participants, other)
                .join(other.user, u)
                .where(
                        c.type.eq(ConversationType.DM),
                        me.user.id.eq(requesterId),
                        other.user.id.ne(requesterId),
                        other.user.id.eq(withUserId)
                )
                .fetchOne();

        return Optional.ofNullable(result)
                .map(row -> toDto(row, requesterId));
    }

    @Override
    public boolean existsParticipant(
            UUID conversationId,
            UUID userId
    ) {
        Integer result = queryFactory
                .selectOne()
                .from(cp)
                .where(
                        cp.conversation.id.eq(conversationId),
                        cp.user.id.eq(userId)
                )
                .fetchFirst();

        return result != null;
    }

    private ConversationDto toDto(Tuple row, UUID requesterId) {
        Conversation conversation = row.get(c);
        User withUser = row.get(u);

        Message lastestMessage = findLastestMessage(conversation.getId());
        boolean hasUnread = hasUnreadMessage(conversation.getId(), requesterId);

        return conversationMapper.toDto(
                conversation,
                withUser,
                lastestMessage,
                hasUnread
        );
    }

    private Map<UUID, Message> findLastestMessage(
            List<UUID> conversationIds
    ) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }

        QMessage newer = new QMessage("newer");

        List<Tuple> results = queryFactory
                .select(m.conversation.id, m)
                .from(m)
                .leftJoin(m.sender, sender).fetchJoin()
                .leftJoin(m.receiver, receiver).fetchJoin()
                .where(
                    m.conversation.id.in(conversationIds),
                    JPAExpressions
                        .selectOne()
                        .from(newer)
                        .where(
                            newer.conversation.id.eq(m.conversation.id),
                            newer.createdAt.gt(m.createdAt)
                                .or(
                                    newer.createdAt.eq(m.createdAt)
                                    .and(newer.id.gt(m.id))
                                )
                        )
                        .notExists()
                ).fetch();

        Map<UUID, Message> lastestMessages = new HashMap<>();

        for (Tuple result : results) {
            lastestMessages.put(
                    result.get(m.conversation.id),
                    result.get(m)
            );
        }

        return lastestMessages;
    }

    private Set<UUID> findUnreadConversationIds(
            List<UUID> conversationIds,
            UUID requesterId
    ) {
        if (conversationIds.isEmpty()) {
            return Set.of();
        }

        List<UUID> unreadConversationIds = queryFactory
                .select(m.conversation.id)
                .from(m)
                .where(
                        m.conversation.id.in(conversationIds),
                        m.receiver.id.eq(requesterId),
                        m.readAt.isNull()
                )
                .groupBy(m.conversation.id)
                .fetch();

        return new HashSet<>(unreadConversationIds);
    }

    private Message findLastestMessage(
            UUID conversationId
    ) {
        return queryFactory
                .selectFrom(m)
                .leftJoin(m.sender, sender).fetchJoin()
                .leftJoin(m.receiver, receiver).fetchJoin()
                .where(m.conversation.id.eq(conversationId))
                .orderBy(
                        m.createdAt.desc(),
                        m.id.desc()
                )
                .fetchFirst();
    }

    private boolean hasUnreadMessage(
            UUID conversationId,
            UUID requesterId
    ) {
        Integer result = queryFactory
                .selectOne()
                .from(m)
                .where(
                        m.conversation.id.eq(conversationId),
                        m.receiver.id.eq(requesterId),
                        m.readAt.isNull()
                )
                .fetchFirst();

        return result != null;
    }

    private BooleanExpression keywordCondition(
            String keyword
    ) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return u.username.containsIgnoreCase(keyword.trim());
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
                ? c.createdAt.gt(cursor)
                : c.createdAt.lt(cursor);

        // 보조 커서 입력 확인
        if (request.idAfter() == null) {
            return createdAtCondition;
        }

        // 생성 시간이 같은 경우 보조 커서
        BooleanExpression sameCreatedAtCondition = c.createdAt.eq(cursor)
                .and(ascending
                        ? c.id.gt(request.idAfter())
                        : c.id.lt(request.idAfter()));

        return createdAtCondition.or(sameCreatedAtCondition);
    }

    private boolean isAscending(SliceCursorRequest request) {
        if (request.limit() <= 0) {
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
