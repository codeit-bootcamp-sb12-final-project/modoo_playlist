package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository;

import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.CursorResponseWatchingSessionDto;

import java.util.Optional;
import java.util.UUID;

public interface WatchingSessionRepositoryCustom {

    Optional<WatchingSession> findActiveByWatcherId(UUID watcherId);

    CursorResponseWatchingSessionDto findActiveByContent(UUID contentId, String watcherNameLike, SliceCursorRequest request);
}
