package com.codeit.modoo_playlist.moduleapi.dto;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record WatchingSessionDto (
        UUID id,
        Instant createdAt,
        User watcher,
        Content content
){
}
