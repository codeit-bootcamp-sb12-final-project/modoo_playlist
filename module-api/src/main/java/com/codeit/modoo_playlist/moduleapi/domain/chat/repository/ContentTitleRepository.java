package com.codeit.modoo_playlist.moduleapi.domain.chat.repository;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** 챗봇 툴이 제목으로 콘텐츠를 찾을 때 쓰는 조회 전용 저장소. */
public interface ContentTitleRepository extends Repository<Content, UUID> {

  // 글자와 숫자만 남겨 비교한다(공백·구두점·대소문자 무시). ContentRefResolver의 정규화와 같은 규칙이어야 한다.
  @Query("select new com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto("
      + "c.id, c.title, c.thumbnailUrl) "
      + "from Content c where c.deletedAt is null "
      + "and function('regexp_replace', lower(c.title), '[^[:alnum:]]', '') = :title")
  List<ContentCardDto> findCardsByNormalizedTitle(@Param("title") String normalizedTitle);
}
