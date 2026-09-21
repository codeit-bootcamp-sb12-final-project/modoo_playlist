package com.codeit.modoo_playlist.moduleapi.domain.notification.event;

import java.util.UUID;

/**
 * 실시간 시청 세션 시작 시 발행되는 도메인 이벤트.
 * 알림 도메인이 watcherId의 팔로워 목록을 조회해서 "팔로우한 사용자의 실시간 시청 시작" 활동 알림을 보낸다.
 */
public record WatchingSessionStartedEvent(UUID watchingSessionId, UUID watcherId, UUID contentId) {
}