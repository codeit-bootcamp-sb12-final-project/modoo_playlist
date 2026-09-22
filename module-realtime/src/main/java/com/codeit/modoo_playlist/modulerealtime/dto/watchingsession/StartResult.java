package com.codeit.modoo_playlist.modulerealtime.dto.watchingsession;

import java.util.List;
import java.util.UUID;

public record StartResult(
            UUID watchingSessionId,
            List<WatchingSessionChange> changes
    ) {
    }