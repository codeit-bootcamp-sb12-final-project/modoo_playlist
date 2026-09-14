package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository;

import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WatchingSessionRepository
        extends JpaRepository<WatchingSession, UUID>, WatchingSessionRepositoryCustom {

    List<WatchingSession> findByWatcher_IdAndEndedAtIsNull(UUID watcherId);

    long countByContent_IdAndEndedAtIsNull(UUID contentId);
}
