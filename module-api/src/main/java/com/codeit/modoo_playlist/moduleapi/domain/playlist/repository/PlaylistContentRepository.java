package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository;

import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistContent;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistContentId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, PlaylistContentId> {

    List<PlaylistContent> findAllById_PlaylistIdOrderByCreatedAtAsc(UUID playlistId);

}