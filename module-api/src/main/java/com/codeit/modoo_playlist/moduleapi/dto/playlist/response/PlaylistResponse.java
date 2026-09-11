package com.codeit.modoo_playlist.moduleapi.dto.playlist.response;

import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentSummaryResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistResponse(
        UUID id,
        UserSummaryResponse owner,
        String title,
        String description,
        Instant updatedAt,
        long subscriberCount,
        boolean subscribedByMe,
        List<ContentSummaryResponse> contents
) {
}