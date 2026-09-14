package com.codeit.modoo_playlist.moduleapi.dto.chat;

import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;

public record ContentChatDto(
        UserSummaryResponse sender,
        String content
) {
}
