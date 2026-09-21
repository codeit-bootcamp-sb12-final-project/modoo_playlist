package com.codeit.modoo_playlist.moduleapi.domain.notification.listener;

import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.FollowRepository;
import com.codeit.modoo_playlist.moduleapi.domain.notification.event.*;
import com.codeit.modoo_playlist.moduleapi.domain.notification.service.NotificationService;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.PlaylistSubscriptionRepository;
import java.util.List;
import java.util.UUID;
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

    private final NotificationService notificationService;
    private final FollowRepository followRepository;
    private final PlaylistSubscriptionRepository playlistSubscriptionRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFollowed(FollowedEvent event) {
        notificationService.create(
                event.followeeId(),
                "새로운 팔로워",
                "회원님을 팔로우했습니다.",
                NotificationLevel.INFO,
                event.followerId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlaylistCreated(PlaylistCreatedEvent event) {
        List<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(event.ownerId());
        notificationService.createBatch(
                followerIds,
                "팔로우한 사용자의 새 플레이리스트",
                "회원님이 팔로우한 사용자가 새 플레이리스트를 만들었습니다.",
                NotificationLevel.INFO,
                event.playlistId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlaylistSubscribed(PlaylistSubscribedEvent event) {
        notificationService.create(
                event.ownerId(),
                "플레이리스트 구독",
                "회원님의 플레이리스트를 누군가 구독했습니다.",
                NotificationLevel.INFO,
                event.playlistId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlaylistContentAdded(PlaylistContentAddedEvent event) {
        List<UUID> subscriberIds =
                playlistSubscriptionRepository.findSubscriberIdsByPlaylistId(event.playlistId());
        notificationService.createBatch(
                subscriberIds,
                "구독 중인 플레이리스트에 콘텐츠 추가",
                "구독 중인 플레이리스트에 새 콘텐츠가 추가됐습니다.",
                NotificationLevel.INFO,
                event.playlistId()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWatchingSessionStarted(WatchingSessionStartedEvent event) {
        List<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(event.watcherId());
        notificationService.createBatch(
                followerIds,
                "팔로우한 사용자의 실시간 시청",
                "회원님이 팔로우한 사용자가 콘텐츠를 시청하기 시작했습니다.",
                NotificationLevel.INFO,
                event.contentId()
        );
    }

}