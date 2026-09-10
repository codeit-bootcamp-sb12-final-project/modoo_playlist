package com.codeit.modoo_playlist.moduleapi.domain.playlist.service.impl;

import com.codeit.modoo_playlist.core.domain.playlist.entity.GeneratedBy;
import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistContent;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistContentId;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistSubscription;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistSubscriptionId;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistSubscriptionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.service.PlaylistService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistContentRepository playlistContentRepository;
    private final PlaylistSubscriptionRepository playlistSubscriptionRepository;

    @Override
    @Transactional
    public UUID createPlaylist(UUID ownerId, String title, String description) {
        Playlist playlist = Playlist.builder()
                .ownerId(ownerId)
                .title(title)
                .description(description)
                .generatedBy(GeneratedBy.USER)
                .build();

        return playlistRepository.save(playlist).getId();
    }

    @Override
    @Transactional
    public void updatePlaylist(UUID playlistId, UUID ownerId, String title, String description) {
        Playlist playlist = getOwnedPlaylist(playlistId, ownerId);
        playlist.update(title, description);
    }

    @Override
    @Transactional
    public void deletePlaylist(UUID playlistId, UUID ownerId) {
        Playlist playlist = getOwnedPlaylist(playlistId, ownerId);
        playlistRepository.delete(playlist);
    }

    @Override
    public Playlist getPlaylist(UUID playlistId) {
        return playlistRepository.findById(playlistId)
                .orElseThrow(() -> new BaseException(ErrorCode.PLAYLIST_NOT_FOUND));
    }

    @Override
    @Transactional
    public void addContent(UUID playlistId, UUID ownerId, UUID contentId) {
        validateOwner(playlistId, ownerId);

        PlaylistContentId id = new PlaylistContentId(playlistId, contentId);

        if (playlistContentRepository.existsById(id)) {
            throw new BaseException(ErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS);
        }

        PlaylistContent playlistContent = PlaylistContent.builder()
                .id(id)
                .build();

        playlistContentRepository.save(playlistContent);
    }

    @Override
    @Transactional
    public void removeContent(UUID playlistId, UUID ownerId, UUID contentId) {
        validateOwner(playlistId, ownerId);

        PlaylistContentId id = new PlaylistContentId(playlistId, contentId);

        PlaylistContent playlistContent = playlistContentRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.PLAYLIST_CONTENT_NOT_FOUND));

        playlistContentRepository.delete(playlistContent);
    }

    @Override
    @Transactional
    public void subscribe(UUID playlistId, UUID subscriberId) {
        if (!playlistRepository.existsById(playlistId)) {
            throw new BaseException(ErrorCode.PLAYLIST_NOT_FOUND);
        }

        PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, subscriberId);

        if (playlistSubscriptionRepository.existsById(id)) {
            throw new BaseException(ErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS);
        }

        PlaylistSubscription subscription = PlaylistSubscription.builder()
                .id(id)
                .build();

        playlistSubscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public void unsubscribe(UUID playlistId, UUID subscriberId) {
        PlaylistSubscriptionId id = new PlaylistSubscriptionId(playlistId, subscriberId);

        PlaylistSubscription subscription = playlistSubscriptionRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND));

        playlistSubscriptionRepository.delete(subscription);
    }

    @Override
    public boolean isSubscribedByMe(UUID playlistId, UUID subscriberId) {
        return playlistSubscriptionRepository.existsById(new PlaylistSubscriptionId(playlistId, subscriberId));
    }

    @Override
    public long countSubscribers(UUID playlistId) {
        return playlistSubscriptionRepository.countById_PlaylistId(playlistId);
    }

    private Playlist getOwnedPlaylist(UUID playlistId, UUID ownerId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new BaseException(ErrorCode.PLAYLIST_NOT_FOUND));

        if (!playlist.getOwnerId().equals(ownerId)) {
            throw new BaseException(ErrorCode.PLAYLIST_ACCESS_DENIED);
        }

        return playlist;
    }

    private void validateOwner(UUID playlistId, UUID ownerId) {
        getOwnedPlaylist(playlistId, ownerId);
    }

}