package com.codeit.modoo_playlist.moduleapi.dto;

import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentSummaryResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record WatchingSessionDto (
        UUID id,
        Instant createdAt,
        UserSummaryResponse watcher,
        ContentSummaryResponse content
){
}
