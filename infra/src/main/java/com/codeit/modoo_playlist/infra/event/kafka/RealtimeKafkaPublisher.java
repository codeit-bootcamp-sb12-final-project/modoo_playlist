package com.codeit.modoo_playlist.infra.event.kafka;

import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Profile({"prod","docker"})
@Component
@RequiredArgsConstructor
public class RealtimeKafkaPublisher implements RealtimeNotifier {

    public static final String STOMP_TOPIC = "stomp-broadcast";
    public static final String SSE_TOPIC = "sse-broadcast";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void notifyStomp(String destination, Object payload) {

        StompKafkaEvent event = new StompKafkaEvent(destination, payload);

        kafkaTemplate.send(STOMP_TOPIC, destination, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("[Kafka] STOMP 브로드캐스트 전송 실패, destination={}, reason={}", destination, ex.toString());
                    } else {
                        log.debug("[Kafka] STOMP 브로드캐스트 전송 성공, destination={}", destination);
                    }
                });
    }

    @Override
    public void notifySse(Set<UUID> receiverIds, String eventName, Object payload) {

        SseKafkaEvent event = new SseKafkaEvent(receiverIds, eventName, payload);
        String key = UUID.randomUUID().toString();

        kafkaTemplate.send(SSE_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("[Kafka] SSE 알림 전송 실패, eventName={}, reason={}", eventName, ex.toString());
                    } else {
                        log.debug("[Kafka] SSE 알림 전송 성공, eventName={}, receivers={}", eventName, receiverIds);
                    }
                });
    }
}
