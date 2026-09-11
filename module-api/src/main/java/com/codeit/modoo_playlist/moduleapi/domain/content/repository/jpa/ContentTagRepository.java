package com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarContentDto;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentTagQueryRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentTagRepository extends JpaRepository<ContentTag, ContentTagId>, ContentTagQueryRepository {

  @Query("""
      select contentTag
      from ContentTag contentTag
      join fetch contentTag.tag
      where contentTag.id.contentId in :contentIds
      """)
  List<ContentTag> findAllWithTagByContentIds(@Param("contentIds") Collection<UUID> contentIds);

  @Query("""
      select new com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarContentDto(
      	c.id, c.title, c.thumbnailUrl, cast(count(ct2) as double)
      )
      from ContentTag ct1
      join ContentTag ct2 on ct1.tag.id = ct2.tag.id and ct2.content.id != :contentId
      join Content c on c.id = ct2.content.id
      where ct1.content.id = :contentId
      group by c.id, c.title, c.thumbnailUrl
      order by count(ct2) desc, c.id asc
      """)
  List<SimilarContentDto> findSimilarContents(@Param("contentId") UUID contentId,
      Pageable pageable);
}
