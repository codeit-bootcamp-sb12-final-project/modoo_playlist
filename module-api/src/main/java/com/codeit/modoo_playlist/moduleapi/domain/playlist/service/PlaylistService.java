package com.codeit.modoo_playlist.moduleapi.domain.playlist.service;

import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import java.util.UUID;

public interface PlaylistService {

    UUID createPlaylist(UUID ownerId, String title, String description);

    void updatePlaylist(UUID playlistId, UUID ownerId, String title, String description);

    void deletePlaylist(UUID playlistId, UUID ownerId);

    Playlist getPlaylist(UUID playlistId);

    void addContent(UUID playlistId, UUID ownerId, UUID contentId);

    void removeContent(UUID playlistId, UUID ownerId, UUID contentId);

    void subscribe(UUID playlistId, UUID subscriberId);

    void unsubscribe(UUID playlistId, UUID subscriberId);

    boolean isSubscribedByMe(UUID playlistId, UUID subscriberId);

    long countSubscribers(UUID playlistId);

}