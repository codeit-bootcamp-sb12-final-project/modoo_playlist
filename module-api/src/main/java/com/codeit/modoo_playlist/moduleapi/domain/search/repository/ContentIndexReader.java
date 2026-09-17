package com.codeit.modoo_playlist.moduleapi.domain.search.repository;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentDocumentMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
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

    Optional<Content> optionalContent = contentRepository.findByIdAndDeletedAtIsNull(contentId);

    if (optionalContent.isEmpty()) {
      return Optional.empty();
    }

    Content content = optionalContent.get();

    List<ContentTag> contentTags = contentTagRepository.findAllWithTagByContentIds(List.of(contentId));

    List<String> tagNames = contentTags.stream()
        .map(contentTag -> contentTag.getTag().getName())
        .toList();

    long watcherCount = readWatcherCounts(List.of(contentId))
        .getOrDefault(contentId, 0L);

    return Optional.of(
        contentDocumentMapper.toDocument(content, tagNames, watcherCount)
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

    TypedQuery<Content> query = entityManager.createQuery(jpql, Content.class);

    if (lastId != null) {
      query.setParameter("lastId", lastId);
    }

    List<Content> contents = query.setMaxResults(size).getResultList();

    if (contents.isEmpty()) {
      return List.of();
    }

    List<UUID> contentIds = contents.stream()
        .map(Content::getId)
        .toList();

    Map<UUID, List<ContentTag>> tagsByContentId =
        contentTagRepository.findAllWithTagByContentIds(contentIds)
            .stream()
            .collect(Collectors.groupingBy(
                contentTag -> contentTag.getId().getContentId()
            ));

    Map<UUID, Long> watcherCountsByContentId =
        readWatcherCounts(contentIds);

    return contents.stream()
        .map(content -> {
          List<ContentTag> contentTags =
              tagsByContentId.getOrDefault(content.getId(), List.of());

          List<String> tagNames = contentTags.stream()
              .map(contentTag -> contentTag.getTag().getName())
              .toList();

          return contentDocumentMapper.toDocument(
              content,
              tagNames,
              watcherCountsByContentId.getOrDefault(content.getId(), 0L)
          );
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public long readWatcherCount(UUID contentId) {
    Objects.requireNonNull(contentId, "콘텐츠 ID는 필수입니다.");

    return readWatcherCounts(List.of(contentId)).getOrDefault(contentId, 0L);
  }

  private Map<UUID, Long> readWatcherCounts(List<UUID> contentIds) {
    if (contentIds.isEmpty()) {
      return Map.of();
    }

    List<Tuple> rows = entityManager.createQuery("""
            select
                watchingSession.content.id,
                count(distinct watchingSession.watcher.id)
            from WatchingSession watchingSession
            where watchingSession.content.id in :contentIds
                and watchingSession.endedAt is null
            group by watchingSession.content.id
            """, Tuple.class)
        .setParameter("contentIds", contentIds)
        .getResultList();

    return rows.stream()
        .collect(Collectors.toMap(
            row -> row.get(0, UUID.class),
            row -> row.get(1, Long.class)
        ));
  }
}