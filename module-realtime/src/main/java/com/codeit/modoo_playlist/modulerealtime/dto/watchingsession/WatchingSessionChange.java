package com.codeit.modoo_playlist.modulerealtime.dto.watchingsession;

import com.codeit.modoo_playlist.core.domain.watchingSession.dto.WatchingSessionDto;

public record WatchingSessionChange(
        ChangeType type,
        WatchingSessionDto watchingSession,
        long watcherCount
) {
}
