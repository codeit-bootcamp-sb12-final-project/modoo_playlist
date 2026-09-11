package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query;

import java.util.Objects;
import java.util.UUID;

public record PlaylistListCondition(
        UUID ownerId,
        UUID subscriberId,
        String keyword,
        String cursor,
        UUID idAfter,
        int limit,
        SortType sortBy,
        SortDirection sortDirection
) {

    public PlaylistListCondition {
        keyword = normalize(keyword);
        cursor = normalize(cursor);
        sortBy = Objects.requireNonNull(sortBy, "sortBy는 필수입니다.");
        sortDirection = Objects.requireNonNull(sortDirection, "sortDirection은 필수입니다.");

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit은 1 이상 100 이하여야 합니다.");
        }

        if ((cursor == null) != (idAfter == null)) {
            throw new IllegalArgumentException("cursor와 idAfter는 함께 전달해야 합니다.");
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public enum SortType {
        UPDATED_AT,
        CREATED_AT
    }

    public enum SortDirection {
        ASCENDING,
        DESCENDING
    }
}