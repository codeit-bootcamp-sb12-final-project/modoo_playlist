package com.codeit.modoo_playlist.infra.client.sportsdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportsDbEvent(
        String idEvent,
        String strEvent,
        String strLeague,
        String strSeason,
        String strSport,
        String strHomeTeam,
        String strAwayTeam,
        String strVenue,
        String strTimestamp,
        String strCountry,
        String strThumb,
        String strLeagueBadge,
        String strHomeTeamBadge,
        String strStatus,
        String strPostponed
) {
}
