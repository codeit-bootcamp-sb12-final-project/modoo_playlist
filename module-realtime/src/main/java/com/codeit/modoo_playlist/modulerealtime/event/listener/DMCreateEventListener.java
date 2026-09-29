package com.codeit.modoo_playlist.modulerealtime.event.listener;


import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.modulerealtime.event.DMCreateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DMCreateEventListener {

    private static final String NOTIFICATION_ASYNC_EXECUTOR = "notificationAsyncExecutor";
    private final RealtimeNotifier realtimeNotifier;

    @Async(NOTIFICATION_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handles(DMCreateEvent event){
        realtimeNotifier.notifySse(
                Set.of(event.receiverId()),
                "direct-messages",
                event.message()
        );
    }
}
