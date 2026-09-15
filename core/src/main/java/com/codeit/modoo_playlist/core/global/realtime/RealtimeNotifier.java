package com.codeit.modoo_playlist.core.global.realtime;

public interface RealtimeNotifier {

    void notifyStomp(String destination, Object payload);
}