package com.codeit.modoo_playlist.moduleapi.domain.playlist.service;

import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.request.PlaylistListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.response.PlaylistCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.response.PlaylistResponse;

import java.util.UUID;

public interface PlaylistService {

    UUID createPlaylist(UUID ownerId, String title, String description);

    PlaylistResponse updatePlaylist(UUID playlistId, UUID ownerId, String title, String description);

    void deletePlaylist(UUID playlistId, UUID ownerId);

    Playlist getPlaylist(UUID playlistId);

    void addContent(UUID playlistId, UUID ownerId, UUID contentId);

    void removeContent(UUID playlistId, UUID ownerId, UUID contentId);

    void subscribe(UUID playlistId, UUID subscriberId);

    void unsubscribe(UUID playlistId, UUID subscriberId);

    boolean isSubscribedByMe(UUID playlistId, UUID subscriberId);

    long countSubscribers(UUID playlistId);

    PlaylistResponse getPlaylistResponse(UUID playlistId, UUID viewerId);

    PlaylistCursorResponse getPlaylists(PlaylistListRequest request, UUID viewerId);

}