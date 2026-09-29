package com.codeit.modoo_playlist.core.global.realtime;

import java.util.Set;
import java.util.UUID;

public interface RealtimeNotifier {

    void notifyStomp(String destination, Object payload);

    void notifySse(Set<UUID> receiverIds, String eventName, Object message);
}