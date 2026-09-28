package com.codeit.modoo_playlist.infra.event;

import java.util.UUID;

public record DMSentEvent(
        UUID receiverId,
        UUID ownerId,
        String content,
        UUID messageId
){
}
