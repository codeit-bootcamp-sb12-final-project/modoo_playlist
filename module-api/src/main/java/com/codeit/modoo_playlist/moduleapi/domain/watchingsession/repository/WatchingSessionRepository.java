package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository;

import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchingSessionRepository
        extends JpaRepository<WatchingSession, UUID>, WatchingSessionRepositoryCustom {

    List<WatchingSession> findByWatcher_IdAndEndedAtIsNull(UUID watcherId);

    long countDistinctByContent_IdAndEndedAtIsNull(UUID contentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<WatchingSession> findByEndedAtIsNullAndUpdatedAtBefore(Instant cutoff);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WatchingSession> findWatchingSessionById(UUID id);

    @Modifying(flushAutomatically = true)
    @Query("delete from WatchingSession ws where ws.watcher.id = :userId")
    int deleteAllByWatcherId(@Param("userId") UUID userId);

    @Query("""
        select new com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto(
          ws.content.id, ws.content.title, ws.content.thumbnailUrl, cast(count(ws) as double)
        )
        from WatchingSession ws
        where ws.endedAt is null
          and ws.content.deletedAt is null
        group by ws.content.id
        order by count(ws) desc
        """)
    List<RecommendedContentDto> findLiveWatchingContents(Pageable pageable);
}
