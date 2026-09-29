package com.codeit.modoo_playlist.infra.event.kafka;

public record StompKafkaEvent(
        String destination,
        Object payload
) {
}