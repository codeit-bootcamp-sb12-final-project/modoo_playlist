package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.codeit.modoo_playlist.core.domain.content.type.ContentType;

public record ContentListCondition(
        ContentType type,
        String keyword,
        List<String> tagNames,
        String cursor,
        UUID idAfter,
        int limit,
        SortType sortBy,
        SortDirection sortDirection
) {

    public ContentListCondition {
        keyword = normalize(keyword);
        tagNames = tagNames == null
                ? List.of()
                : tagNames.stream()
                        .map(ContentListCondition::normalize)
                        .filter(value -> value != null)
                        .distinct()
                        .toList();
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
        WATCHER_COUNT,
        CREATED_AT,
        AVERAGE_RATING
    }

    public enum SortDirection {
        ASCENDING,
        DESCENDING
    }
}
