package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.service;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.watchingSession.dto.WatchingSessionDto;
import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.mapper.ContentSummaryMapper;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.infra.event.kafka.WatchingSessionStartedEvent;
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
