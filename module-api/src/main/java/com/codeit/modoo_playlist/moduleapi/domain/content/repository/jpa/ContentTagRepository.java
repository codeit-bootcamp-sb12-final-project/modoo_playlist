package com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;

public interface ContentTagRepository extends JpaRepository<ContentTag, ContentTagId> {

    @Query("""
            select contentTag
            from ContentTag contentTag
            join fetch contentTag.tag
            where contentTag.id.contentId in :contentIds
            """)
    List<ContentTag> findAllWithTagByContentIds(@Param("contentIds") Collection<UUID> contentIds);
}
