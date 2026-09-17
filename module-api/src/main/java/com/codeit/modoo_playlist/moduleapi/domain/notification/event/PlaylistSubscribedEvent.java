package com.codeit.modoo_playlist.moduleapi.domain.notification.event;

import java.util.UUID;

/**
 * 플레이리스트 구독 시 발행되는 도메인 이벤트.
 * 알림 도메인이 ownerId(플레이리스트 소유자)에게 "누가 내 플레이리스트를 구독했다" 알림을 보낸다.
 */
public record PlaylistSubscribedEvent(UUID playlistId, UUID ownerId, UUID subscriberId) {
}