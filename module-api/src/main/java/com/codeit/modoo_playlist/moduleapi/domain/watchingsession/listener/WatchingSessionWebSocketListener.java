package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.listener;

import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.service.WatchingSessionRegistry;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionWebSocketListener {

    private static final Pattern WATCH = Pattern.compile(
            "^/sub/contents/"
                    + "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{12})/watch$"
    );

    private final WatchingSessionRegistry registry;

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headers =
                StompHeaderAccessor.wrap(event.getMessage());

        String destination = headers.getDestination();
        String sessionId = headers.getSessionId();
        String subscriptionId = headers.getSubscriptionId();

        if (destination == null
                || sessionId == null
                || subscriptionId == null) {
            return;
        }

        var matcher = WATCH.matcher(destination);

        if (!matcher.matches()) {
            return;
        }

        if (!(event.getUser() instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails user)) {
            log.warn("인증되지 않은 watching subscription: sessionId={}, destination={}",
                    sessionId,
                    destination
            );
            return;
        }

        registry.start(
                user.getUserDto().id(),
                UUID.fromString(matcher.group(1)),
                sessionId,
                subscriptionId
        );
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headers =
                StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = headers.getSessionId();
        String subscriptionId = headers.getSubscriptionId();

        if (sessionId == null || subscriptionId == null) {
            return;
        }

        registry.end(sessionId, subscriptionId);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        registry.endAll(event.getSessionId());
    }
}