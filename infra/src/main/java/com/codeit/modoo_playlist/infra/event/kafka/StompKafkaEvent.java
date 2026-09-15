package com.codeit.modoo_playlist.infra.event.kafka;

//import com.codeit.modoo_playlist.core.global.common.dto.MessageResponse;
import com.fasterxml.jackson.databind.JsonNode;

public record StompKafkaEvent(
        String destination,
        JsonNode payload
) {
}