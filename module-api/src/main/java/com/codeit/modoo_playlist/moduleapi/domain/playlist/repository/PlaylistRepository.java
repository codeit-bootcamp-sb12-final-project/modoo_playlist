package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository;

import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query.PlaylistQueryRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID>, PlaylistQueryRepository {

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

}