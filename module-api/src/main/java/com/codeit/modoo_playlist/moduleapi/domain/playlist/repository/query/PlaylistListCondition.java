package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query;

import java.util.UUID;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

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
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static BaseException invalid(String field) {
        BaseException exception = new BaseException(ErrorCode.PLAYLIST_QUERY_INVALID);
        exception.addDetail("field", field);
        return exception;
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