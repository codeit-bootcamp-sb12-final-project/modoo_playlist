package com.codeit.modoo_playlist.infra.event.kafka;

import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Profile({"prod","docker"})
@Component
@RequiredArgsConstructor
public class RealtimeKafkaPublisher implements RealtimeNotifier {

    public static final String STOMP_TOPIC = "stomp-broadcast";

    private final KafkaTemplate<String, StompKafkaEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void notifyStomp(String destination, Object payload) {
        JsonNode jsonPayload = objectMapper.valueToTree(payload);

        StompKafkaEvent event =
                new StompKafkaEvent(destination, jsonPayload);

        kafkaTemplate.send(STOMP_TOPIC, destination, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("[Kafka] STOMP 브로드캐스트 전송 실패, destination={}, reason={}", destination, ex.toString());
                    } else {
                        log.debug("[Kafka] STOMP 브로드캐스트 전송 성공, destination={}", destination);
                    }
                });
    }
}
