package com.codeit.modoo_playlist.moduleapi.dto.content.request;

import java.time.Instant;

import com.codeit.modoo_playlist.core.domain.content.type.SportsStatus;

import jakarta.validation.constraints.Size;

public record ContentSportsRequest(
        @Size(max = 50) String sportType,
        @Size(max = 100) String league,
        @Size(max = 20) String season,
        @Size(max = 100) String homeTeam,
        @Size(max = 100) String awayTeam,
        @Size(max = 100) String venue,
        SportsStatus status,
        Instant kickoffAt
) {
}
