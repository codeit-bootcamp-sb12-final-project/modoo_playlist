package com.codeit.modoo_playlist.modulerealtime.event;

import com.codeit.modoo_playlist.core.domain.message.entity.MessageDto;

import java.util.UUID;

public record DMCreateEvent(
        UUID receiverId,
        MessageDto message
) {
}
