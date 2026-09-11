package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;

public interface ContentTagQueryRepository {

    List<ContentTag> findAllWithTagByContentIds(Collection<UUID> contentIds);
}
