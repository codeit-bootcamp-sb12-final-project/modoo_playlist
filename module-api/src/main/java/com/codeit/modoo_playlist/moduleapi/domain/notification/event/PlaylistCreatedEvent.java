package com.codeit.modoo_playlist.moduleapi.domain.notification.event;

import java.util.UUID;

/**
 * 플레이리스트 생성 시 발행되는 도메인 이벤트.
 * 알림 도메인이 ownerId의 팔로워 목록을 조회해서 "팔로우한 사용자의 신규 플레이리스트 생성" 활동 알림을 보낸다.
 */
public record PlaylistCreatedEvent(UUID playlistId, UUID ownerId) {
}