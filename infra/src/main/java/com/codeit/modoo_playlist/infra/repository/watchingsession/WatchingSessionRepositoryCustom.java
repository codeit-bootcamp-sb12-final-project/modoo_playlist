package com.codeit.modoo_playlist.infra.repository.watchingsession;

import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.core.domain.watchingSession.dto.CursorResponseWatchingSessionDto;
import com.codeit.modoo_playlist.core.global.common.dto.base.SliceCursorRequest;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface WatchingSessionRepositoryCustom {

    Optional<WatchingSession> findActiveByWatcherId(UUID watcherId);

    CursorResponseWatchingSessionDto findActiveByContent(UUID contentId, String watcherNameLike, SliceCursorRequest request);

    void touchActiveSessions(Collection<UUID> sessionIds, Instant touchedAt);
}
