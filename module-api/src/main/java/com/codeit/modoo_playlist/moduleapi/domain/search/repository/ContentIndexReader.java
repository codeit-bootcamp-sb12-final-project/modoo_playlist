package com.codeit.modoo_playlist.moduleapi.domain.search.repository;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentDocumentMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ContentIndexReader {

  private final EntityManager entityManager;
  private final ContentTagRepository contentTagRepository;
  private final ContentDocumentMapper contentDocumentMapper;

  @Transactional(readOnly = true)
  public List<ContentDocument> read(int start, int size) {

    List<Content> contents = entityManager.createQuery("""
            select content
            from Content content
            where content.deletedAt is null
            order by content.id
            """, Content.class)
        .setFirstResult(start)
        .setMaxResults(size)
        .getResultList();

    if (contents.isEmpty()) {
      return List.of();
    }

    List<UUID> contentIds = contents.stream()
        .map(Content::getId)
        .toList();

    Map<UUID, List<String>> tagsByContentId = contentTagRepository
        .findAllWithTagByContentIds(contentIds)
        .stream()
        .collect(Collectors.groupingBy(
            contentTag -> contentTag.getId().getContentId(),
            Collectors.mapping(
                contentTag -> contentTag.getTag().getName(),
                Collectors.toList()
            )
        ));

    return contents.stream()
        .map(content -> contentDocumentMapper.toDocument(
            content,
            tagsByContentId.getOrDefault(content.getId(), List.of())
        ))
        .toList();
  }

}
