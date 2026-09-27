package com.codeit.modoo_playlist.modulerealtime.event;


import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DMCreateEventListener {

    private final RealtimeNotifier realtimeNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handles(DMCreateEvent event){
        realtimeNotifier.notifySse(
                Set.of(event.receiverId()),
                "direct-messages",
                event.message()
        );
    }
}
