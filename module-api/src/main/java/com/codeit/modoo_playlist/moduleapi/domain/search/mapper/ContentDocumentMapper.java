package com.codeit.modoo_playlist.moduleapi.domain.search.mapper;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ContentDocumentMapper {

  public ContentDocument toDocument(Content content, List<String> tagNames, long watcherCount) {
    Objects.requireNonNull(content, "콘텐츠는 필수입니다.");
    Objects.requireNonNull(content.getId(), "저장된 콘텐츠의 ID는 필수입니다.");
    Objects.requireNonNull(tagNames, "태그명 목록은 필수입니다.");

    if (watcherCount < 0) {
      throw new IllegalArgumentException("시청자 수는 0 이상이어야 합니다.");
    }

    return ContentDocument.builder()
        .id(content.getId().toString())
        .type(content.getType())
        .title(content.getTitle())
        .description(content.getDescription())
        .thumbnailUrl(content.getThumbnailUrl())
        .tags(List.copyOf(tagNames))
        .averageRating(content.getAverageRating().doubleValue())
        .reviewCount(content.getReviewCount())
        .watcherCount(watcherCount)
        .createdAt(content.getCreatedAt())
        .build();
  }

}
