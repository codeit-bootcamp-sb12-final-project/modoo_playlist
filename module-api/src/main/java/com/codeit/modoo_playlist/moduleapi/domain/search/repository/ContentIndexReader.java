package com.codeit.modoo_playlist.moduleapi.domain.search.repository;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentDocumentMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ContentIndexReader {

  private final EntityManager entityManager;
  private final ContentRepository contentRepository;
  private final ContentTagRepository contentTagRepository;
  private final ContentDocumentMapper contentDocumentMapper;

  @Transactional(readOnly = true)
  public Optional<ContentDocument> readOne(UUID contentId) {
    Objects.requireNonNull(contentId, "콘텐츠 ID는 필수입니다.");

    Optional<Content> optionalContent =
        contentRepository.findByIdAndDeletedAtIsNull(contentId);

    if (optionalContent.isEmpty()) {
      return Optional.empty();
    }

    Content content = optionalContent.get();

    List<String> tagNames = contentTagRepository
        .findAllWithTagByContentIds(List.of(contentId))
        .stream()
        .map(contentTag -> contentTag.getTag().getName())
        .toList();

    return Optional.of(
        contentDocumentMapper.toDocument(content, tagNames)
    );
  }

  @Transactional(readOnly = true)
  public List<ContentDocument> read(UUID lastId, int size) {

    if (size <= 0) {
      throw new IllegalArgumentException("조회 개수는 1 이상이어야 합니다.");
    }

    String jpql = lastId == null
        ? """
            select content
            from Content content
            where content.deletedAt is null
            order by content.id asc
            """
        : """
            select content
            from Content content
            where content.deletedAt is null
                and content.id > :lastId
            order by content.id asc
            """;

    TypedQuery<Content> query =
        entityManager.createQuery(jpql, Content.class);

    if (lastId != null) {
      query.setParameter("lastId", lastId);
    }

    List<Content> contents = query
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
