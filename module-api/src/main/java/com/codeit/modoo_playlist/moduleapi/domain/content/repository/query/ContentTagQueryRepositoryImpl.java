package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import static com.codeit.modoo_playlist.core.domain.content.entity.QContent.content;
import static com.codeit.modoo_playlist.core.domain.content.entity.QContentTag.contentTag;
import static com.codeit.modoo_playlist.core.domain.tag.entity.QTag.tag;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.QContentTag;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarContentDto;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;

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

    @Override
    public List<SimilarContentDto> findSimilarContents(UUID contentId, Pageable pageable) {
        QContentTag targetContentTag = new QContentTag("targetContentTag");
        QContentTag candidateContentTag = new QContentTag("candidateContentTag");

        return queryFactory
                .select(Projections.constructor(
                        SimilarContentDto.class,
                        content.id,
                        content.title,
                        content.thumbnailUrl,
                        candidateContentTag.count().doubleValue()
                ))
                .from(targetContentTag)
                .join(candidateContentTag)
                .on(targetContentTag.tag.id.eq(candidateContentTag.tag.id)
                        .and(candidateContentTag.content.id.ne(contentId)))
                .join(content)
                .on(content.id.eq(candidateContentTag.content.id))
                .where(targetContentTag.content.id.eq(contentId))
                .groupBy(content.id, content.title, content.thumbnailUrl)
                .orderBy(candidateContentTag.count().desc(), content.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public void increaseTagContentCounts(Collection<UUID> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }

        queryFactory
                .update(tag)
                .set(tag.contentCount, tag.contentCount.add(1))
                .where(tag.id.in(tagIds))
                .execute();
    }

    @Override
    public void decreaseTagContentCounts(Collection<UUID> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }

        queryFactory
                .update(tag)
                .set(tag.contentCount, tag.contentCount.subtract(1))
                .where(tag.id.in(tagIds), tag.contentCount.gt(0))
                .execute();
    }
}
