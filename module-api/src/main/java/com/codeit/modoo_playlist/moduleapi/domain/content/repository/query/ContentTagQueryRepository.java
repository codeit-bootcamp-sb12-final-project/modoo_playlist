package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarContentDto;

import org.springframework.data.domain.Pageable;

public interface ContentTagQueryRepository {

    List<ContentTag> findAllWithTagByContentIds(Collection<UUID> contentIds);

    List<SimilarContentDto> findSimilarContents(UUID contentId, Pageable pageable);

    void increaseTagContentCounts(Collection<UUID> tagIds);

    void decreaseTagContentCounts(Collection<UUID> tagIds);
}
