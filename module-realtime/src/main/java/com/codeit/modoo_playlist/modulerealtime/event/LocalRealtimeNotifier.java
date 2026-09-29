package com.codeit.modoo_playlist.modulerealtime.event;

import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.infra.event.kafka.RealtimeKafkaPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@Profile("!prod & !docker")
public class LocalRealtimeNotifier implements RealtimeNotifier {

    public LocalRealtimeNotifier(
            SimpMessagingTemplate messagingTemplate,
            KafkaTemplate<String,Object> kafkaTemplate
    ){
        this.messagingTemplate = messagingTemplate;
        this.kafkaPublisher = new RealtimeKafkaPublisher(kafkaTemplate);
    }

    private final SimpMessagingTemplate messagingTemplate;
    private final RealtimeKafkaPublisher kafkaPublisher;

    @Override
    public void notifyStomp(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    @Override
    public void notifySse(Set<UUID> receiverIds, String eventName, Object message) {
        kafkaPublisher.notifySse(receiverIds, eventName, message);

    }
}