package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentIndexReader;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentIndexService {

  private final ContentIndexReader contentIndexReader;
  private final ContentSearchRepository contentSearchRepository;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void index(UUID contentId) {
    Objects.requireNonNull(contentId, "콘텐츠 ID는 필수입니다.");

    Optional<ContentDocument> optionalDocument =
        contentIndexReader.readOne(contentId);

    if (optionalDocument.isEmpty()) {
      contentSearchRepository.deleteById(contentId.toString());
      return;
    }

    contentSearchRepository.save(optionalDocument.get());
  }
}
