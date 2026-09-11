package com.codeit.modoo_playlist.moduleapi.dto.conversation.response;

import com.codeit.modoo_playlist.core.domain.conversation.entity.SortDirection;
import com.codeit.modoo_playlist.moduleapi.dto.ConversationDto;
import com.querydsl.core.types.Order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CursorResponseConversationDto(
        List<ConversationDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        SortDirection sortDirection
) {
}
