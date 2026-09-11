package com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentTagQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentTagRepository
        extends JpaRepository<ContentTag, ContentTagId>, ContentTagQueryRepository {
}
