package com.codeit.modoo_playlist.moduleapi.domain.review.repository.query;

import java.util.UUID;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

public record ReviewListCondition(
        UUID contentId,
        String cursor,
        UUID idAfter,
        int limit,
        SortType sortBy,
        SortDirection sortDirection
) {
    public ReviewListCondition {
        cursor = normalize(cursor);

        if (sortBy == null) {
            throw invalid("sortBy");
        }
        if (sortDirection == null) {
            throw invalid("sortDirection");
        }
        if (limit < 1 || limit > 100) {
            throw invalid("limit");
        }
        if ((cursor == null) != (idAfter == null)) {
            throw invalid("cursor");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BaseException invalid(String field) {
        BaseException exception = new BaseException(ErrorCode.REVIEW_QUERY_INVALID);
        exception.addDetail("field", field);
        return exception;
    }

    public enum SortType {
        CREATED_AT
    }

    public enum SortDirection {
        ASCENDING, DESCENDING
    }
}