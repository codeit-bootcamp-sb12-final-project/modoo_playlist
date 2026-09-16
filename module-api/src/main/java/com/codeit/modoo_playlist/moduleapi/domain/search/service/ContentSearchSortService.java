package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContentSearchSortService {

  private static final String ID_SORT_FIELD = "id.keyword";

  public List<SortOptions> createSortOptions(String sortBy, String sortDirection) {
    if (sortBy == null || sortBy.isBlank()) {
      if (sortDirection != null && !sortDirection.isBlank()) {
        throw new IllegalArgumentException("정렬 방향을 지정하려면 sortBy도 전달해야 합니다.");
      }
      return List.of();
    }

    String resolvedSortBy = sortBy.trim();
    SortOrder order = resolveSortOrder(sortDirection);

    if ("recommended".equals(resolvedSortBy)) {
      SortOptions scoreSort = SortOptions.of(s -> s.score(score -> score.order(order)));
      SortOptions idSort = createIdSort(order);
      return List.of(scoreSort, idSort);
    }

    String field = switch (resolvedSortBy) {
      case "watcherCount" -> "watcherCount";
      case "createdAt" -> "createdAt";
      case "rate", "averageRating" -> "averageRating";
      default -> throw new IllegalArgumentException("지원하지 않는 검색 정렬입니다: " + sortBy);
    };

    SortOptions primarySort = SortOptions.of(s -> s.field(f -> {
      f.field(field).order(order);
      if ("createdAt".equals(field)) {
        f.format("strict_date_optional_time_nanos");
      }
      return f;
    }));

    SortOptions idSort = createIdSort(order);
    return List.of(primarySort, idSort);
  }

  private SortOptions createIdSort(SortOrder order) {
    return SortOptions.of(s -> s.field(f -> f.field(ID_SORT_FIELD).order(order)));
  }

  private SortOrder resolveSortOrder(String sortDirection) {
    return switch (resolveDirection(sortDirection)) {
      case "ASCENDING" -> SortOrder.Asc;
      case "DESCENDING" -> SortOrder.Desc;
      default -> throw new IllegalArgumentException("sortDirection은 ASCENDING 또는 DESCENDING이어야 합니다.");
    };
  }

  public Object parseCursor(String cursor, String sortBy) {
    try {
      if ("createdAt".equals(sortBy)) {
        return Instant.parse(cursor).toString();
      }

      if ("watcherCount".equals(sortBy)) {
        long watcherCount = Long.parseLong(cursor);

        if (watcherCount < 0) {
          throw new IllegalArgumentException("시청자 수 커서는 0 이상이어야 합니다.");
        }

        return watcherCount;
      }

      double value = Double.parseDouble(cursor);

      if (!Double.isFinite(value)) {
        throw new IllegalArgumentException(sortBy + " 커서는 유한한 숫자여야 합니다.");
      }

      return value;
    } catch (DateTimeParseException | NumberFormatException exception) {
      throw new IllegalArgumentException(sortBy + " 커서 형식이 올바르지 않습니다.", exception);
    }
  }

  public String resolveDirection(String sortDirection) {
    String direction = trimToNull(sortDirection);
    return direction == null ? "DESCENDING" : direction;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
