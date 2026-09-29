package com.codeit.modoo_playlist.infra.event;

import java.util.UUID;

public record WatcherCountChangedEvent(
        UUID contentId
) {
    public static final String TOPIC = "watcher-count-changed";
}
