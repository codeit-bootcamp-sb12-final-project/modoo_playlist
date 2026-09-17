package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;

import org.springframework.data.domain.Pageable;

public interface ContentTagQueryRepository {

    List<ContentTag> findAllWithTagByContentIds(Collection<UUID> contentIds);

    List<RecommendedContentDto> findSimilarContents(UUID contentId, Pageable pageable);

    List<RecommendedContentDto> findContentsByTagId(UUID tagId, Pageable pageable);

    void increaseTagContentCounts(Collection<UUID> tagIds);

    void decreaseTagContentCounts(Collection<UUID> tagIds);
}
