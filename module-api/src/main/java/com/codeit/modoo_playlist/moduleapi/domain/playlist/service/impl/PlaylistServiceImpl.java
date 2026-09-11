package com.codeit.modoo_playlist.moduleapi.domain.playlist.service.impl;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.playlist.entity.GeneratedBy;
import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistContent;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistContentId;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistSubscription;
import com.codeit.modoo_playlist.core.domain.playlist.entity.PlaylistSubscriptionId;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.mapper.ContentMapper;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.mapper.PlaylistMapper;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistSubscriptionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query.PlaylistListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query.PlaylistQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.service.PlaylistService;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentSummaryResponse;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.request.PlaylistListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.response.PlaylistCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.response.PlaylistResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistContentRepository playlistContentRepository;
    private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ContentTagRepository contentTagRepository;
    private final PlaylistMapper playlistMapper;
    private final ContentMapper contentMapper;

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
    public PlaylistResponse updatePlaylist(UUID playlistId, UUID ownerId, String title, String description) {
        Playlist playlist = getOwnedPlaylist(playlistId, ownerId);
        playlist.update(title, description);
        return getPlaylistResponse(playlistId, ownerId);
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

    @Override
    public PlaylistResponse getPlaylistResponse(UUID playlistId, UUID viewerId) {
        Playlist playlist = getPlaylist(playlistId);

        User owner = userRepository.findById(playlist.getOwnerId())
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        UserSummaryResponse ownerSummary = new UserSummaryResponse(
                owner.getId(),
                owner.getUsername(),
                owner.getProfileImageUrl()
        );

        long subscriberCount = playlistSubscriptionRepository.countById_PlaylistId(playlistId);
        boolean subscribedByMe = viewerId != null
                && playlistSubscriptionRepository.existsById(new PlaylistSubscriptionId(playlistId, viewerId));

        List<UUID> contentIds = playlistContentRepository
                .findAllById_PlaylistIdOrderByCreatedAtAsc(playlistId).stream()
                .map(playlistContent -> playlistContent.getId().getContentId())
                .toList();

        Map<UUID, Content> contentsById = contentRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(Content::getId, content -> content));

        Map<UUID, List<String>> tagsByContentId = loadTagsByContentId(contentIds);

        List<ContentSummaryResponse> contents = contentIds.stream()
                .map(contentsById::get)
                .filter(Objects::nonNull)
                .map(content -> contentMapper.toSummary(
                        content,
                        tagsByContentId.getOrDefault(content.getId(), List.of())
                ))
                .toList();

        return playlistMapper.toResponse(playlist, ownerSummary, subscriberCount, subscribedByMe, contents);
    }

    @Override
    public PlaylistCursorResponse getPlaylists(PlaylistListRequest request, UUID viewerId) {
        PlaylistListCondition condition = toCondition(request);

        PlaylistQueryPage page = playlistRepository.findAllByCondition(condition);
        List<Playlist> playlists = page.playlists();

        if (playlists.isEmpty()) {
            return playlistMapper.toCursorResponse(
                    page, List.of(), request.sortBy(), request.sortDirection());
        }

        List<UUID> playlistIds = playlists.stream().map(Playlist::getId).toList();

        Map<UUID, User> ownersById = userRepository
                .findAllById(playlists.stream().map(Playlist::getOwnerId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<PlaylistSubscription> subscriptions = playlistSubscriptionRepository
                .findAllById_PlaylistIdIn(playlistIds);
        Map<UUID, Long> subscriberCountByPlaylistId = subscriptions.stream()
                .collect(Collectors.groupingBy(
                        subscription -> subscription.getId().getPlaylistId(),
                        Collectors.counting()
                ));
        Set<UUID> subscribedPlaylistIds = (viewerId == null)
                ? Set.of()
                : subscriptions.stream()
                .filter(subscription -> subscription.getId().getSubscriberId().equals(viewerId))
                .map(subscription -> subscription.getId().getPlaylistId())
                .collect(Collectors.toSet());

        List<PlaylistContent> playlistContents = playlistContentRepository
                .findAllById_PlaylistIdInOrderByCreatedAtAsc(playlistIds);
        Map<UUID, List<UUID>> contentIdsByPlaylistId = playlistContents.stream()
                .collect(Collectors.groupingBy(
                        playlistContent -> playlistContent.getId().getPlaylistId(),
                        Collectors.mapping(
                                playlistContent -> playlistContent.getId().getContentId(),
                                Collectors.toList()
                        )
                ));

        List<UUID> allContentIds = playlistContents.stream()
                .map(playlistContent -> playlistContent.getId().getContentId())
                .distinct()
                .toList();

        Map<UUID, Content> contentsById = contentRepository.findAllById(allContentIds).stream()
                .collect(Collectors.toMap(Content::getId, content -> content));

        Map<UUID, List<String>> tagsByContentId = loadTagsByContentId(allContentIds);

        List<PlaylistResponse> data = playlists.stream()
                .map(playlist -> {
                    User owner = ownersById.get(playlist.getOwnerId());
                    UserSummaryResponse ownerSummary = (owner == null)
                            ? null
                            : new UserSummaryResponse(owner.getId(), owner.getUsername(), owner.getProfileImageUrl());
                    long subscriberCount = subscriberCountByPlaylistId.getOrDefault(playlist.getId(), 0L);
                    boolean subscribedByMe = subscribedPlaylistIds.contains(playlist.getId());
                    List<ContentSummaryResponse> contents = contentIdsByPlaylistId
                            .getOrDefault(playlist.getId(), List.of()).stream()
                            .map(contentsById::get)
                            .filter(Objects::nonNull)
                            .map(content -> contentMapper.toSummary(
                                    content,
                                    tagsByContentId.getOrDefault(content.getId(), List.of())
                            ))
                            .toList();
                    return playlistMapper.toResponse(playlist, ownerSummary, subscriberCount, subscribedByMe, contents);
                })
                .toList();

        return playlistMapper.toCursorResponse(page, data, request.sortBy(), request.sortDirection());
    }

    private PlaylistListCondition toCondition(PlaylistListRequest request) {
        return new PlaylistListCondition(
                request.ownerIdEqual(),
                request.subscriberIdEqual(),
                request.keywordLike(),
                request.cursor(),
                request.idAfter(),
                request.limit(),
                parseSortType(request.sortBy()),
                PlaylistListCondition.SortDirection.valueOf(request.sortDirection())
        );
    }

    private PlaylistListCondition.SortType parseSortType(String sortBy) {
        return switch (sortBy) {
            case "updatedAt" -> PlaylistListCondition.SortType.UPDATED_AT;
            case "createdAt" -> PlaylistListCondition.SortType.CREATED_AT;
            default -> throw new IllegalArgumentException("지원하지 않는 sortBy 값입니다: " + sortBy);
        };
    }

    private Map<UUID, List<String>> loadTagsByContentId(List<UUID> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }

        return contentTagRepository.findAllWithTagByContentIds(contentIds).stream()
                .collect(Collectors.groupingBy(
                        contentTag -> contentTag.getId().getContentId(),
                        Collectors.mapping(
                                contentTag -> contentTag.getTag().getName(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        names -> names.stream().sorted().toList()
                                )
                        )
                ));
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