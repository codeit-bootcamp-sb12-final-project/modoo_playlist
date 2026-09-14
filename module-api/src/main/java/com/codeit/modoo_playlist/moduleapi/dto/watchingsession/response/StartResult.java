package com.codeit.modoo_playlist.moduleapi.dto.watchingsession.response;

import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.WatchingSessionChange;

import java.util.List;
import java.util.UUID;

public record StartResult(
            UUID watchingSessionId,
            List<WatchingSessionChange> changes
    ) {
    }