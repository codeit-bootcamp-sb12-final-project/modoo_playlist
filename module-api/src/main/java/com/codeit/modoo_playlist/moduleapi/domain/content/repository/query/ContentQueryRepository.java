package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import java.util.UUID;

public interface ContentQueryRepository {

    ContentQueryPage findAllByCondition(ContentListCondition condition);

    ContentQueryPage findAllByCondition(ContentListCondition condition, UUID userId);

    long countCurrentWatchers(UUID contentId);
}
