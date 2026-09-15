package com.codeit.modoo_playlist.moduleapi.dto.watchingsession;

import com.codeit.modoo_playlist.moduleapi.dto.WatchingSessionDto;

public record WatchingSessionChange(
        ChangeType type,
        WatchingSessionDto watchingSession,
        long watcherCount
) {
}
