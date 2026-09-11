package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import java.util.List;
import java.util.UUID;

import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

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
        if (sortBy == null || sortDirection == null) {
            throw invalidQuery("sort", null);
        }

        if (limit < 1 || limit > 100) {
            throw invalidQuery("limit", limit);
        }

        if ((cursor == null) != (idAfter == null)) {
            throw invalidQuery("cursor", cursor);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static BaseException invalidQuery(String field, Object value) {
        BaseException exception = new BaseException(ErrorCode.CONTENT_QUERY_INVALID);
        exception.addDetail("field", field);
        exception.addDetail("value", value);
        return exception;
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
