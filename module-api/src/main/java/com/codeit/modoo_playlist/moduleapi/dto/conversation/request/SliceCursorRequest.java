package com.codeit.modoo_playlist.moduleapi.dto.conversation.request;

import java.util.UUID;

public record SliceCursorRequest(
        String keywordLike,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
) {
}
