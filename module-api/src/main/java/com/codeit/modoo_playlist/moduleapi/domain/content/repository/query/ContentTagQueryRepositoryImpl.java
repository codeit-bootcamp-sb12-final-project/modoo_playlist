package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import static com.codeit.modoo_playlist.core.domain.content.entity.QContentTag.contentTag;
import static com.codeit.modoo_playlist.core.domain.tag.entity.QTag.tag;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.querydsl.jpa.impl.JPAQueryFactory;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;

import jakarta.persistence.EntityManager;

public class ContentTagQueryRepositoryImpl implements ContentTagQueryRepository {

    private final JPAQueryFactory queryFactory;

    public ContentTagQueryRepositoryImpl(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public List<ContentTag> findAllWithTagByContentIds(Collection<UUID> contentIds) {
        if (contentIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(contentTag)
                .join(contentTag.tag, tag).fetchJoin()
                .where(contentTag.id.contentId.in(contentIds))
                .fetch();
    }
}
