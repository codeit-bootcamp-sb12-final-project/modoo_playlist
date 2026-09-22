package com.codeit.modoo_playlist.modulerealtime.watchingSession.service;

import com.codeit.modoo_playlist.core.domain.content.dto.ContentSummaryResponse;
import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.watchingSession.dto.WatchingSessionDto;
import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.repository.RealtimeContentRepository;
import com.codeit.modoo_playlist.infra.repository.RealtimeContentTagRepository;
import com.codeit.modoo_playlist.infra.repository.RealtimeUserRepository;
import com.codeit.modoo_playlist.infra.repository.watchingsession.WatchingSessionRepository;
import com.codeit.modoo_playlist.infra.mapper.ContentSummaryMapper;
import com.codeit.modoo_playlist.infra.mapper.WatchingSessionMapper;
import com.codeit.modoo_playlist.modulerealtime.dto.watchingsession.ChangeType;
import com.codeit.modoo_playlist.modulerealtime.dto.watchingsession.StartResult;
import com.codeit.modoo_playlist.modulerealtime.dto.watchingsession.WatchingSessionChange;
import com.codeit.modoo_playlist.infra.event.kafka.WatchingSessionStartedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchingSessionCommandService {

    private final RealtimeUserRepository userRepository;
    private final WatchingSessionRepository watchingSessionRepository;
    private final RealtimeContentRepository contentRepository;
    private final RealtimeContentTagRepository contentTagRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final WatchingSessionMapper watchingSessionMapper;
    private final ContentSummaryMapper contentSummaryMapper;

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
        watchingSessionRepository.save(session);

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

        if (staleSessions.isEmpty()) {
            return List.of();
        }

        List<UUID> contentIds = staleSessions.stream()
                .map(session -> session.getContent().getId())
                .distinct()
                .toList();
        Map<UUID, List<String>> tagsByContentId = contentTagRepository
                .findAllWithTagByContentIds(contentIds).stream()
                .collect(Collectors.groupingBy(
                        contentTag -> contentTag.getId().getContentId(),
                        Collectors.mapping(contentTag -> contentTag.getTag().getName(), Collectors.toList())
                ));

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

            Content content = session.getContent();
            ContentSummaryResponse summary = contentSummaryMapper.toSummary(
                    content,
                    tagsByContentId.getOrDefault(content.getId(), List.of())
            );
            changes.add(change(ChangeType.LEAVE, session, watchingSessionMapper.toDto(session, summary)));
        }

        return List.copyOf(changes);
    }

    private WatchingSessionChange change(
            ChangeType type,
            WatchingSession session
    ) {
        return change(type, session, toDto(session));
    }

    private WatchingSessionChange change(
            ChangeType type,
            WatchingSession session,
            WatchingSessionDto dto
    ) {
        long watcherCount = watchingSessionRepository
                .countDistinctByContent_IdAndEndedAtIsNull(
                        session.getContent().getId()
                );

        return new WatchingSessionChange(
                type,
                dto,
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
