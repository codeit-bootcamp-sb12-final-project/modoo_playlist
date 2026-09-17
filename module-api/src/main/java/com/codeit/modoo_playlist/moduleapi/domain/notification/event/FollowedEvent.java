package com.codeit.modoo_playlist.moduleapi.domain.notification.event;

import java.util.UUID;

/**
 * 팔로우 발생 시 발행되는 도메인 이벤트.
 * 알림 도메인이 이 이벤트를 구독해서 followeeId(팔로우 당한 사람)에게 알림을 생성/전송한다.
 */
public record FollowedEvent(UUID followerId, UUID followeeId) {
}