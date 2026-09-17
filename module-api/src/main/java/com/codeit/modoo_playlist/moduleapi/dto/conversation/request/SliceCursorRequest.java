package com.codeit.modoo_playlist.moduleapi.dto.conversation.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record SliceCursorRequest(
        String cursor,
        UUID idAfter,
        @Min(1) @Max(100)
        int limit,
        String sortDirection,
        String sortBy
) {
        @AssertTrue(message = "cursor와 idAfter는 함께 전달되어야 합니다.")
        public boolean isCursorPairValid() {
                boolean hasCursor = cursor != null && !cursor.isBlank();
                boolean hasIdAfter = idAfter != null;

                return hasCursor == hasIdAfter;
        }
}
