
package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository;

import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.infra.repository.watchingsession.WatchingSessionRepositoryCustom;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ApiWatchingSessionRepository extends JpaRepository<WatchingSession, UUID>, WatchingSessionRepositoryCustom {

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