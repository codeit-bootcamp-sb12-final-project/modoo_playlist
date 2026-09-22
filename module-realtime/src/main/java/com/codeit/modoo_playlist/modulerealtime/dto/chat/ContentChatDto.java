package com.codeit.modoo_playlist.modulerealtime.dto.chat;

import com.codeit.modoo_playlist.core.domain.user.dto.UserSummaryResponse;

public record ContentChatDto(
        UserSummaryResponse sender,
        String content
) {
}
