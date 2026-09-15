package com.codeit.modoo_playlist.moduleapi.domain.search.mapper;

import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentListItemResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ContentSearchResponseMapper {

  private final EntityManager entityManager;

  // 활성 DB 콘텐츠만 기존 ES 검색 순서를 유지하며 응답 DTO로 변환
  @Transactional(readOnly = true)
  public List<ContentListItemResponse> toResponses(
      List<ContentDocument> documents
  ) {
    if (documents.isEmpty()) {
      return List.of();
    }

    List<UUID> contentIds = documents.stream()
        .map(document -> UUID.fromString(document.getId()))
        .toList();

    List<Tuple> rows = entityManager.createQuery("""
            select
                content.id,
                content.releaseDate,
                count(distinct session.watcher.id)
            from Content content
            left join WatchingSession session
                on session.content = content
                and session.endedAt is null
            where content.id in :contentIds
                and content.deletedAt is null
            group by content.id, content.releaseDate
            """, Tuple.class)
        .setParameter("contentIds", contentIds)
        .getResultList();

    Map<UUID, Tuple> rowsById = rows.stream()
        .collect(Collectors.toMap(
            row -> row.get(0, UUID.class),
            row -> row
        ));

    return documents.stream()
        .filter(document ->
            rowsById.containsKey(UUID.fromString(document.getId()))
        )
        .map(document -> {
          UUID contentId = UUID.fromString(document.getId());
          Tuple row = rowsById.get(contentId);

          String type = switch (document.getType()) {
            case MOVIE -> "movie";
            case TV -> "tvSeries";
            case SPORT -> "sport";
          };

          return new ContentListItemResponse(
              contentId,
              type,
              document.getTitle(),
              document.getDescription(),
              document.getThumbnailUrl(),
              row.get(1, LocalDate.class),
              document.getTags(),
              BigDecimal.valueOf(document.getAverageRating()),
              document.getReviewCount(),
              row.get(2, Long.class)
          );
        })
        .toList();
  }

}
