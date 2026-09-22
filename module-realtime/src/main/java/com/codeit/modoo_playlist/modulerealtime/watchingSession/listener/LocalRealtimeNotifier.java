package com.codeit.modoo_playlist.modulerealtime.watchingSession.listener;

import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod & !docker")
@RequiredArgsConstructor
public class LocalRealtimeNotifier implements RealtimeNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void notifyStomp(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }
}