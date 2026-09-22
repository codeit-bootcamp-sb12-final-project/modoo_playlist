package com.codeit.modoo_playlist.core.domain.watchingSession.dto;
import com.codeit.modoo_playlist.core.domain.content.dto.ContentSummaryResponse;
import com.codeit.modoo_playlist.core.domain.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record WatchingSessionDto (
        UUID id,
        Instant createdAt,
        UserSummaryResponse watcher,
        ContentSummaryResponse content
){
}
