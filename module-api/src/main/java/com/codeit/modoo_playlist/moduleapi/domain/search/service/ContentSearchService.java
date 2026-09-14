package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.global.common.util.KeywordNormalizer;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentSearchResponseMapper;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentSearchService {

  private static final int DEFAULT_SEARCH_LIMIT = 20;
  private static final int MAX_SEARCH_LIMIT = 100;
  private static final String ID_SORT_FIELD = "id.keyword";

  private final ElasticsearchOperations elasticsearchOperations;
  private final ContentSearchResponseMapper contentSearchResponseMapper;

  // 검색어 전용 호출
  public SearchHits<ContentDocument> search(String rawKeyword) {
    String keyword = KeywordNormalizer.normalize(rawKeyword);

    if (keyword.isBlank()) {
      throw new IllegalArgumentException("검색어는 비어 있을 수 없습니다.");
    }

    return search(keyword, null, List.of(), DEFAULT_SEARCH_LIMIT);
  }

  // 검색어와 필터 조건으로 검색
  public SearchHits<ContentDocument> search(
      String rawKeyword,
      String typeEqual,
      List<String> tagsIn,
      Integer limit
  ) {
    return search(rawKeyword, typeEqual, tagsIn, limit, null, null);
  }

  // 검색어·필터·정렬 조건으로 검색
  public SearchHits<ContentDocument> search(
      String rawKeyword,
      String typeEqual,
      List<String> tagsIn,
      Integer limit,
      String sortBy,
      String sortDirection
  ) {
    int searchLimit = resolveLimit(limit);
    List<SortOptions> sorts = createSortOptions(sortBy, sortDirection);

    NativeQueryBuilder queryBuilder =
        createQueryBuilder(rawKeyword, typeEqual, tagsIn)
            .withPageable(PageRequest.of(0, searchLimit));

    if (!sorts.isEmpty()) {
      queryBuilder.withSort(sorts);
    }

    return elasticsearchOperations.search(
        queryBuilder.build(),
        ContentDocument.class
    );
  }

  // 기존 콘텐츠 요청·응답 DTO를 사용하는 커서 조회
  public ContentCursorResponse searchPage(ContentListRequest request) {
    Objects.requireNonNull(request, "검색 요청은 필수입니다.");

    int limit = resolveLimit(request.limit());
    String sortBy = trimToNull(request.sortBy());

    if (sortBy == null) {
      throw new IllegalArgumentException(
          "커서 조회에는 createdAt, rate, averageRating 중 sortBy를 지정해야 합니다."
      );
    }

    String sortDirection = resolveDirection(request.sortDirection());
    List<SortOptions> sorts = createSortOptions(sortBy, sortDirection);

    String cursor = trimToNull(request.cursor());
    UUID idAfter = request.idAfter();

    if ((cursor == null) != (idAfter == null)) {
      throw new IllegalArgumentException(
          "cursor와 idAfter는 함께 전달해야 합니다."
      );
    }

    NativeQueryBuilder queryBuilder = createQueryBuilder(
        request.keywordLike(),
        request.typeEqual(),
        request.tagsIn()
    )
        .withSort(sorts)
        .withPageable(PageRequest.of(0, limit + 1))
        .withTrackTotalHits(true);

    if (cursor != null) {
      queryBuilder.withSearchAfter(List.of(
          parseCursor(cursor, sortBy),
          idAfter.toString()
      ));
    }

    SearchHits<ContentDocument> searchHits = elasticsearchOperations.search(
        queryBuilder.build(),
        ContentDocument.class
    );

    List<SearchHit<ContentDocument>> hits = searchHits.getSearchHits();
    boolean hasNext = hits.size() > limit;

    List<SearchHit<ContentDocument>> pageHits = hasNext
        ? hits.subList(0, limit)
        : hits;

    List<ContentDocument> documents = pageHits.stream()
        .map(SearchHit::getContent)
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      SearchHit<ContentDocument> last = pageHits.get(pageHits.size() - 1);

      nextCursor = last.getSortValues().get(0).toString();
      nextIdAfter = UUID.fromString(
          last.getSortValues().get(1).toString()
      );
    }

    return new ContentCursorResponse(
        contentSearchResponseMapper.toResponses(documents),
        nextCursor,
        nextIdAfter,
        hasNext,
        searchHits.getTotalHits(),
        sortBy,
        sortDirection
    );
  }

  // 검색어·타입·태그 조건을 공통으로 구성
  private NativeQueryBuilder createQueryBuilder(
      String rawKeyword,
      String typeEqual,
      List<String> tagsIn
  ) {
    String keyword = KeywordNormalizer.normalize(rawKeyword);
    ContentType type = toContentType(typeEqual);
    List<String> tags = normalizeTags(tagsIn);

    BoolQuery.Builder boolQuery = new BoolQuery.Builder();

    if (keyword.isBlank()) {
      boolQuery.must(q -> q.matchAll(m -> m));
    } else {
      boolQuery.must(q -> q.multiMatch(m -> m
          .query(keyword)
          .fields("title", "description")
      ));
    }

    if (type != null) {
      boolQuery.filter(q -> q.term(t -> t
          .field("type")
          .value(type.name())
      ));
    }

    if (!tags.isEmpty()) {
      List<FieldValue> tagValues = tags.stream()
          .map(FieldValue::of)
          .toList();

      boolQuery.filter(q -> q.terms(t -> t
          .field("tags")
          .terms(values -> values.value(tagValues))
      ));
    }

    return NativeQuery.builder()
        .withQuery(q -> q.bool(boolQuery.build()));
  }

  // 주 정렬과 보조 ID 정렬 구성
  private List<SortOptions> createSortOptions(
      String sortBy,
      String sortDirection
  ) {
    if (sortBy == null || sortBy.isBlank()) {
      if (sortDirection != null && !sortDirection.isBlank()) {
        throw new IllegalArgumentException(
            "정렬 방향을 지정하려면 sortBy도 전달해야 합니다."
        );
      }
      return List.of();
    }

    String field = switch (sortBy.trim()) {
      case "createdAt" -> "createdAt";
      case "rate", "averageRating" -> "averageRating";
      default -> throw new IllegalArgumentException(
          "현재 ES 검색은 createdAt, rate, averageRating 정렬을 지원합니다."
      );
    };

    SortOrder order = switch (resolveDirection(sortDirection)) {
      case "ASCENDING" -> SortOrder.Asc;
      case "DESCENDING" -> SortOrder.Desc;
      default -> throw new IllegalArgumentException(
          "sortDirection은 ASCENDING 또는 DESCENDING이어야 합니다."
      );
    };

    SortOptions primarySort = SortOptions.of(s -> s.field(f -> {
      f.field(field).order(order);

      if ("createdAt".equals(field)) {
        f.format("strict_date_optional_time_nanos");
      }

      return f;
    }));

    SortOptions idSort = SortOptions.of(s -> s.field(f -> f
        .field(ID_SORT_FIELD)
        .order(order)
    ));

    return List.of(primarySort, idSort);
  }

  // 문자열 커서를 ES 정렬 필드에 맞는 값으로 변환
  private Object parseCursor(String cursor, String sortBy) {
    try {
      if ("createdAt".equals(sortBy)) {
        return Instant.parse(cursor).toString();
      }

      double rating = Double.parseDouble(cursor);

      if (!Double.isFinite(rating)) {
        throw new IllegalArgumentException(
            "평점 커서는 유한한 숫자여야 합니다."
        );
      }

      return rating;
    } catch (DateTimeParseException | NumberFormatException exception) {
      throw new IllegalArgumentException(
          sortBy + " 커서 형식이 올바르지 않습니다.",
          exception
      );
    }
  }

  private int resolveLimit(Integer limit) {
    int resolved = limit == null ? DEFAULT_SEARCH_LIMIT : limit;

    if (resolved < 1 || resolved > MAX_SEARCH_LIMIT) {
      throw new IllegalArgumentException(
          "limit은 1 이상 100 이하여야 합니다."
      );
    }

    return resolved;
  }

  private String resolveDirection(String sortDirection) {
    String direction = trimToNull(sortDirection);
    return direction == null ? "DESCENDING" : direction;
  }

  private ContentType toContentType(String typeEqual) {
    if (typeEqual == null || typeEqual.isBlank()) {
      return null;
    }

    return switch (typeEqual.trim()) {
      case "movie" -> ContentType.MOVIE;
      case "tvSeries" -> ContentType.TV;
      case "sport" -> ContentType.SPORT;
      default -> throw new IllegalArgumentException(
          "typeEqual은 movie, tvSeries, sport 중 하나여야 합니다."
      );
    };
  }

  private List<String> normalizeTags(List<String> tagsIn) {
    if (tagsIn == null || tagsIn.isEmpty()) {
      return List.of();
    }

    return tagsIn.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(tag -> !tag.isEmpty())
        .distinct()
        .toList();
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }
}
