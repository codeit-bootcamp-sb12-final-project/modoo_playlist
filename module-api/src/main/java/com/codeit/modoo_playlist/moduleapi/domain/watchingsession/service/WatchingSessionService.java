package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.service;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.modoo_playlist.moduleapi.dto.WatchingSessionDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.CursorResponseWatchingSessionDto;

import com.codeit.modoo_playlist.moduleapi.mapper.WatchingSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}