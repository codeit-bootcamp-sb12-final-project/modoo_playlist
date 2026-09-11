package com.codeit.modoo_playlist.infra.client.sportsdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportsDbEventResponse(
        List<SportsDbEvent> events,
        String error,
        @JsonProperty("Message") String message
) {
}
