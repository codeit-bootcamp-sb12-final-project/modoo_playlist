package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.service;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.watchingSession.dto.WatchingSessionDto;
import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.mapper.ContentSummaryMapper;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.notification.event.WatchingSessionStartedEvent;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository.ApiWatchingSessionRepository;
import com.codeit.modoo_playlist.core.domain.content.dto.ContentSummaryResponse;
import com.codeit.modoo_playlist.core.global.common.dto.base.SliceCursorRequest;

import com.codeit.modoo_playlist.core.domain.watchingSession.dto.CursorResponseWatchingSessionDto;
import com.codeit.modoo_playlist.infra.mapper.WatchingSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class WatchingSessionService {

    private final ApiWatchingSessionRepository apiWatchingSessionRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ContentTagRepository contentTagRepository;
    private final ApplicationEventPublisher eventPublisher;


    private final WatchingSessionMapper watchingSessionMapper;
    private final ContentSummaryMapper contentSummaryMapper;

    @Transactional(readOnly = true)
    public WatchingSessionDto findByUser(UUID watcherId) {
        if (watcherId != null) {
            userRepository.findById(watcherId)
                    .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        } else {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }

        return apiWatchingSessionRepository.findActiveByWatcherId(watcherId)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CursorResponseWatchingSessionDto findByContent(
            UUID contentId, String watcherNameLike, SliceCursorRequest request
    ) {
        if (contentId != null) {
            contentRepository.findById(contentId)
                    .orElseThrow(() -> new BaseException(ErrorCode.CONTENT_NOT_FOUND));
        } else {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        return apiWatchingSessionRepository.findActiveByContent(contentId, watcherNameLike, request);
    }

    @Transactional
    public StartResult start(UUID watcherId, UUID contentId) {
        User watcher = userRepository.findById(watcherId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        Content content = contentRepository.findById(contentId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(ErrorCode.CONTENT_NOT_FOUND));

        List<WatchingSessionChange> changes = new ArrayList<>();

        // 새 세션 생성
        WatchingSession session = WatchingSession.builder()
                .watcher(watcher)
                .content(content)
                .startedAt(LocalDateTime.now())
                .build();

        // 새 세션 저장
        watchingSessionRepository.saveAndFlush(session);

        // 팔로워에게 "실시간 시청 시작" 알림을 보내기 위한 이벤트 발행
        eventPublisher.publishEvent(
                new WatchingSessionStartedEvent(session.getId(), watcherId, contentId)
        );

        // JOIN 생성
        changes.add(change(ChangeType.JOIN, session));

        return new StartResult(
                session.getId(),
                List.copyOf(changes)
        );
    }

    @Transactional
    public Optional<WatchingSessionChange> end(UUID watchingSessionId) {
        // 세션 존재 확인
        WatchingSession session = watchingSessionRepository
                .findWatchingSessionById(watchingSessionId)
                .orElse(null);

        // 세션이 없거나 이미 종료면 무시
        if (session == null || !session.isWatching()) {
            return Optional.empty();
        }

        session.end();
        watchingSessionRepository.flush();

        return Optional.of(change(ChangeType.LEAVE, session));
    }

    @Transactional
    public void touch(Collection<UUID> sessionIds) {
        if (sessionIds.isEmpty()) {
            return;
        }

        watchingSessionRepository.touchActiveSessions(
                sessionIds,
                Instant.now()
        );
    }

    @Transactional
    public List<WatchingSessionChange> expireStaleSessions(
            Instant cutoff
    ) {
        List<WatchingSession> staleSessions =
                watchingSessionRepository
                        .findByEndedAtIsNullAndUpdatedAtBefore(cutoff);

        List<WatchingSessionChange> changes = new ArrayList<>();

        for (WatchingSession session : staleSessions) {
            if (!session.isWatching()) {
                continue;
            }

            LocalDateTime lastSeenAt = LocalDateTime.ofInstant(
                    session.getUpdatedAt(),
                    ZoneId.systemDefault() // ZoneOffset.UTC << 서버와 DB의 표준 시간대를 UTC로 통일
            );

            session.end(lastSeenAt);
            watchingSessionRepository.flush();

            changes.add(change(ChangeType.LEAVE, session));
        }

        return List.copyOf(changes);
    }

    private WatchingSessionChange change(
            ChangeType type,
            WatchingSession session
    ) {
        long watcherCount = watchingSessionRepository
                .countDistinctByContent_IdAndEndedAtIsNull(
                        session.getContent().getId()
                );

        return new WatchingSessionChange(
                type,
                toDto(session),
                watcherCount
        );
    }

    private WatchingSessionDto toDto(WatchingSession session){
        Content content = session.getContent();

        List<String> tags = contentTagRepository
                .findAllWithTagByContentIds(List.of(content.getId()))
                .stream()
                .map(contentTag -> contentTag.getTag().getName())
                .toList();

        ContentSummaryResponse summary =
                contentSummaryMapper.toSummary(content,tags);

        return watchingSessionMapper.toDto(session, summary);
    }
}
