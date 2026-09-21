package com.codeit.modoo_playlist.modulerealtime.event;

import com.codeit.modoo_playlist.infra.event.kafka.RealtimeKafkaPublisher;
import com.codeit.modoo_playlist.infra.event.kafka.StompKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Profile({"prod","docker"})
@Component
@RequiredArgsConstructor
public class RealtimeKafkaListener {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = RealtimeKafkaPublisher.STOMP_TOPIC,
//            groupId = "stomp-node-#{T(java.util.UUID).randomUUID().toString()}"
            groupId = "stomp-node-${INSTANCE_ID}"
    )
    public void onStompEvent(StompKafkaEvent event) {
        log.debug("[Kafka] STOMP 이벤트 수신, destination={}", event.destination());
        messagingTemplate.convertAndSend(
                event.destination(),
                event.payload()
        );
    }

}
