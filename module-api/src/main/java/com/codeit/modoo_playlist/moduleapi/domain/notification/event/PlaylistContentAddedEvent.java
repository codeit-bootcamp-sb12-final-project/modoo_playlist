package com.codeit.modoo_playlist.moduleapi.domain.notification.event;

import java.util.UUID;

/**
 * 플레이리스트에 콘텐츠가 추가될 때 발행되는 도메인 이벤트.
 * 알림 도메인이 해당 플레이리스트의 구독자 전체를 조회해서 "구독 중인 플레이리스트에 콘텐츠가 추가됐다" 알림을 보낸다.
 */
public record PlaylistContentAddedEvent(UUID playlistId, UUID contentId) {
}