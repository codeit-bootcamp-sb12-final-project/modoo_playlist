package com.codeit.modoo_playlist.infra.event.kafka;

import java.util.Set;
import java.util.UUID;

public record SseKafkaEvent(
        Set<UUID> receiverIds,
        String eventName,
        Object payload
) {
}
