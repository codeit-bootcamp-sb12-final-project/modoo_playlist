package com.codeit.modoo_playlist.moduleapi.dto.request;

import java.util.UUID;

public record ConversationCreateRequest (
        UUID withUserId
)
{
}
