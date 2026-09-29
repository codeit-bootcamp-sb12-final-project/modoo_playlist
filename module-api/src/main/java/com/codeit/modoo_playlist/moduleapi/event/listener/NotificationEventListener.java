package com.codeit.modoo_playlist.moduleapi.event.listener;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.FollowRepository;
import com.codeit.modoo_playlist.moduleapi.domain.notification.service.NotificationService;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistRepository;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistSubscriptionRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationEventListener {

    private static final String NOTIFICATION_ASYNC_EXECUTOR = "notificationAsyncExecutor";

    private final NotificationService notificationService;
    private final FollowRepository followRepository;
    private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
    private final UserRepository userRepository;
    private final PlaylistRepository playlistRepository;
    private final ContentRepository contentRepository;

    @Async(NOTIFICATION_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFollowed(FollowedEvent event) {

        String senderName = userRepository.findById(event.followerId())
                .map(User::getUsername)
                .orElse("알 수 없음");

        notificationService.create(
                event.followeeId(),
                "새로운 팔로워",
                senderName + "님이 회원님을 팔로우했습니다.",
                NotificationLevel.INFO,
                event.followerId()
        );
    }

    @Async(NOTIFICATION_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlaylistCreated(PlaylistCreatedEvent event) {
        List<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(event.ownerId());

        String ownerName = userRepository.findById(event.ownerId())
                .map(User::getUsername)
                .orElse("알 수 없음");

        notificationService.createBatch(
                followerIds,
                ownerName+"님의 새 플레이리스트",
                ownerName+"님이 새 플레이리스트를 만들었습니다.",
                NotificationLevel.INFO,
                event.playlistId()
        );
    }

    @Async(NOTIFICATION_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlaylistSubscribed(PlaylistSubscribedEvent event) {

        String subscriberName = userRepository.findById(event.subscriberId())
                .map(User::getUsername)
                .orElse("알 수 없음");

        String playlistTitle = playlistRepository.findById(event.playlistId())
                .map(Playlist::getTitle)
                .orElse("알 수 없음");

        notificationService.create(
                event.ownerId(),
                "플레이리스트 구독",
                subscriberName+"님이 회원님의 "+playlistTitle+" (을)를구독했습니다.",
                NotificationLevel.INFO,
                event.playlistId()
        );
    }

    @Async(NOTIFICATION_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlaylistContentAdded(PlaylistContentAddedEvent event) {
        List<UUID> subscriberIds =
                playlistSubscriptionRepository.findSubscriberIdsByPlaylistId(event.playlistId());

        String playlistTitle = playlistRepository.findById(event.playlistId())
                .map(Playlist::getTitle)
                .orElse("알 수 없음");

        String contentTitle = contentRepository.findById(event.contentId())
                .map(Content::getTitle)
                .orElse("알 수 없음");

        String ownerName = userRepository.findById(event.ownerId())
                .map(User::getUsername)
                .orElse("알 수 없음");

        notificationService.createBatch(
                subscriberIds,
                "구독 중인 플레이리스트에 콘텐츠 추가",
                "구독 중인 플레이리스트 "+playlistTitle+"에 새 콘텐츠 "+contentTitle+" (이)가 추가됐습니다.",
                NotificationLevel.INFO,
                event.playlistId()
        );
        Set<UUID> subscriberIdSet = new HashSet<>(subscriberIds);

        List<UUID> followerIds = followRepository
                .findFollowerIdsByFolloweeId(event.ownerId())
                .stream()
                .filter(id -> !subscriberIdSet.contains(id))
                .toList();
        notificationService.createBatch(
                followerIds,
                ownerName+"님의 플레이리스트에 콘텐츠 추가",
                ownerName+"님의 플레이리스트 "+playlistTitle+"에 새 콘텐츠 "+contentTitle+" (을)를 추가했습니다.",
                NotificationLevel.INFO,
                event.playlistId()
        );
    }

    @Async(NOTIFICATION_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuthorizationChanged(AuthorizationChangedEvent event) {
        notificationService.create(
                event.userId(),
                "권한 변경",
                "회원님의 권한이 " + event.newRole().name() + "(으)로 변경되었습니다.",
                NotificationLevel.INFO,
                event.userId()
        );
    }
}