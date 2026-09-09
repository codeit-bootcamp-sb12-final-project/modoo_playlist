package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

public interface ContentQueryRepository {

    ContentQueryPage findAllByCondition(ContentListCondition condition);
}
