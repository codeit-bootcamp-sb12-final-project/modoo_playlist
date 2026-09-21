package com.codeit.modoo_playlist.infra.repository;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RealtimeContentTagRepository extends Repository<ContentTag, ContentTagId> {
    @Query("""
            select ct
            from ContentTag ct
            join fetch ct.tag
            where ct.id.contentId in :contentIds
            """)
    List<ContentTag> findAllWithTagByContentIds(
            @Param("contentIds") Collection<UUID> contentIds
    );
}
