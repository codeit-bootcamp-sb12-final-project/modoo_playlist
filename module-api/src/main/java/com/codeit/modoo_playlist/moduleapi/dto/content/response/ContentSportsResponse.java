package com.codeit.modoo_playlist.moduleapi.dto.content.response;

import java.time.Instant;

import com.codeit.modoo_playlist.core.domain.content.type.SportsStatus;

public record ContentSportsResponse(
        String sportType,
        String league,
        String season,
        String homeTeam,
        String awayTeam,
        String venue,
        SportsStatus status,
        Instant kickoffAt
) {
}
