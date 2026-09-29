package com.codeit.modoo_playlist.modulerealtime.event.listener;

import com.codeit.modoo_playlist.infra.event.kafka.RealtimeKafkaPublisher;
import com.codeit.modoo_playlist.infra.event.kafka.SseKafkaEvent;
import com.codeit.modoo_playlist.modulerealtime.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseKafkaListener {

    private final SseService sseService;

    @KafkaListener(
            topics = RealtimeKafkaPublisher.SSE_TOPIC,
            groupId = "sse-node-${INSTANCE_ID}"
    )
    public void onSseEvent(SseKafkaEvent event) {
        log.debug("[Kafka] SSE 이벤트 수신, eventName={}, receivers={}", event.eventName(), event.receiverIds());
        sseService.send(event.receiverIds(), event.eventName(), event.payload());
    }
}
