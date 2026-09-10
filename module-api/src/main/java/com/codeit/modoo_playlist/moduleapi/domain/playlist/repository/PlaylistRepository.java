package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository;

import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

}