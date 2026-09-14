package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.service;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.modoo_playlist.moduleapi.dto.WatchingSessionDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.*;

import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.response.CursorResponseWatchingSessionDto;
import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.response.StartResult;
import com.codeit.modoo_playlist.moduleapi.mapper.WatchingSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchingSessionService {

    private final WatchingSessionRepository watchingSessionRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    private final WatchingSessionMapper watchingSessionMapper;

    @Transactional(readOnly = true)
    public WatchingSessionDto findByUser(UUID watcherId){
        if(watcherId != null) {
            userRepository.findById(watcherId)
                    .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        }else{
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }

        return watchingSessionRepository.findActiveByWatcherId(watcherId)
                .map(watchingSessionMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CursorResponseWatchingSessionDto findByContent (
            UUID contentId, String watcherNameLike, SliceCursorRequest request
    ){
        if(contentId != null) {
            contentRepository.findById(contentId)
                    .orElseThrow(() -> new BaseException(ErrorCode.CONTENT_NOT_FOUND));
        }else {
            throw new BaseException(ErrorCode.INVALID_REQUEST);
        }
        return watchingSessionRepository.findActiveByContent(contentId, watcherNameLike, request);
    }

    @Transactional
    public StartResult start(UUID watcherId, UUID contentId) {
        User watcher = userRepository.findById(watcherId)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        Content content = contentRepository.findById(contentId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new BaseException(ErrorCode.CONTENT_NOT_FOUND));

        List<WatchingSessionChange> changes = new ArrayList<>();

        // 이전 활성 세션 조회
        List<WatchingSession> previousSessions =
                watchingSessionRepository
                        .findByWatcher_IdAndEndedAtIsNull(watcherId);


        for (WatchingSession previous : previousSessions) {
            previous.end();
            watchingSessionRepository.flush();

            changes.add(change(ChangeType.LEAVE, previous));
        }

        // 새 세션 생성
        WatchingSession session = WatchingSession.builder()
                .watcher(watcher)
                .content(content)
                .startedAt(LocalDateTime.now())
                .build();

        // 새 세션 저장
        watchingSessionRepository.saveAndFlush(session);

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
                .findById(watchingSessionId)
                .orElse(null);

        // 세션이 없거나 이미 종료면 무시
        if (session == null || !session.isWatching()) {
            return Optional.empty();
        }

        session.end();
        watchingSessionRepository.flush();

        return Optional.of(change(ChangeType.LEAVE, session));
    }

    private WatchingSessionChange change(
            ChangeType type,
            WatchingSession session
    ) {
        long watcherCount = watchingSessionRepository
                .countByContent_IdAndEndedAtIsNull(
                        session.getContent().getId()
                );

        return new WatchingSessionChange(
                type,
                watchingSessionMapper.toDto(session),
                watcherCount
        );
    }
}