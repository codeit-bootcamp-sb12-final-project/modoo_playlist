package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentDocumentMapper;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentIndexService {

  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;
  private final ContentDocumentMapper contentDocumentMapper;
  private final ContentSearchRepository contentSearchRepository;

  @Transactional(readOnly = true)
  public void index(UUID contentId) {
    Objects.requireNonNull(contentId, "콘텐츠 ID는 필수입니다.");

    Optional<Content> optionalContent =
        contentRepository.findByIdAndDeletedAtIsNull(contentId);

    if (optionalContent.isEmpty()) {
      contentSearchRepository.deleteById(contentId.toString());
      return;
    }

    Content content = optionalContent.get();

    List<String> tagNames = contentTagRepository
        .findAllWithTagByContentIds(List.of(contentId))
        .stream()
        .map(contentTag -> contentTag.getTag().getName())
        .toList();

    ContentDocument document =
        contentDocumentMapper.toDocument(content, tagNames);

    contentSearchRepository.save(document);
  }

}
