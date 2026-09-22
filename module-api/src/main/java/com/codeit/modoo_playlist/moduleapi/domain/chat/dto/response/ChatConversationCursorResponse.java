package com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response;

import com.codeit.modoo_playlist.core.domain.conversation.entity.SortDirection;
import java.util.List;
import java.util.UUID;

public record ChatConversationCursorResponse(
    List<ChatConversationSummaryDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    SortDirection sortDirection
) {

}
