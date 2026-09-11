package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query;

import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import java.util.List;
import java.util.UUID;

public record PlaylistQueryPage(
        List<Playlist> playlists,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount
) {

    public PlaylistQueryPage {
        playlists = List.copyOf(playlists);
    }
}