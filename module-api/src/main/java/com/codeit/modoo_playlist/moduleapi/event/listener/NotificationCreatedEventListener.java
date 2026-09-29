package com.codeit.modoo_playlist.moduleapi.event.listener;

import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.moduleapi.event.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCreatedEventListener {

    private static final String NOTIFICATION_ASYNC_EXECUTOR = "notificationAsyncExecutor";
    private final RealtimeNotifier realtimeNotifier;

    @Async(NOTIFICATION_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event){
        realtimeNotifier.notifySse(
                Set.of(event.notification().receiverId()),
                "notifications",
                event.notification()
        );
    }
}
