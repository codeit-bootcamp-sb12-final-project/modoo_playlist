package com.codeit.modoo_playlist.moduleapi.dto.conversation.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record SliceCursorRequest(
        String cursor,
        UUID idAfter,
        @Min(0) @Max(100)
        int limit,
        String sortDirection,
        String sortBy
) {
}
