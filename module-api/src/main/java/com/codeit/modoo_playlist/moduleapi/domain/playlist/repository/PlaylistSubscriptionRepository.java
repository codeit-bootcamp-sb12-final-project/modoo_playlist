package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository;

import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistSubscription;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistSubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscription, PlaylistSubscriptionId> {

    long countById_PlaylistId(UUID playlistId);

}