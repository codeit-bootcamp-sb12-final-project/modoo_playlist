package com.codeit.modoo_playlist.moduleapi.domain.preference.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UserPreferenceTagQuery(
    @Min(1)
    @Max(100)
    Integer limit
) {
    public UserPreferenceTagQuery {
        if (limit == null) {
            limit = 10;
        }
    }
}
