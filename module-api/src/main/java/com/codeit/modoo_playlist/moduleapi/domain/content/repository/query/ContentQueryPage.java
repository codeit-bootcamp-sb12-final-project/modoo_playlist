package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import java.util.List;
import java.util.UUID;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;

public record ContentQueryPage(
        List<ContentItem> contents,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount
) {

    public ContentQueryPage {
        contents = List.copyOf(contents);
    }

    public record ContentItem(
            Content content,
            long watcherCount
    ) {
    }
}
