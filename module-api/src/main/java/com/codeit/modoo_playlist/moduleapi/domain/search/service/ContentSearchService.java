package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.global.common.util.KeywordNormalizer;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.event.SearchExecutedEvent;
import com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentSearchResponseMapper;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentListItemResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSearchService {

  private static final int DEFAULT_SEARCH_LIMIT = 20;
  private static final int MAX_SEARCH_LIMIT = 100;
  private static final String DEFAULT_SORT_BY = "watcherCount";

  private final ElasticsearchOperations elasticsearchOperations;
  private final ContentSearchResponseMapper contentSearchResponseMapper;
  private final ContentSearchSortService contentSearchSortService;
  private final ApplicationEventPublisher eventPublisher;

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

    List<SortOptions> sorts = contentSearchSortService.createSortOptions(sortBy, sortDirection);
    NativeQueryBuilder queryBuilder = createQueryBuilder(rawKeyword, typeEqual, tagsIn)
        .withPageable(PageRequest.of(0, searchLimit));

    if (!sorts.isEmpty()) {
      queryBuilder.withSort(sorts);
    }

    return elasticsearchOperations.search(queryBuilder.build(), ContentDocument.class);
  }

  public ContentCursorResponse searchPage(ContentListRequest request) {
    Objects.requireNonNull(request, "검색 요청은 필수입니다.");

    int limit = resolveLimit(request.limit());
    String sortBy = resolvePageSortBy(request.sortBy());
    String sortDirection = contentSearchSortService.resolveDirection(request.sortDirection());

    List<SortOptions> sorts = contentSearchSortService.createSortOptions(sortBy, sortDirection);

    String cursor = trimToNull(request.cursor());
    UUID idAfter = request.idAfter();

    if ((cursor == null) != (idAfter == null)) {
      throw new IllegalArgumentException("cursor와 idAfter는 함께 전달해야 합니다.");
    }

    List<Object> searchAfter = cursor == null
        ? List.of()
        : List.of(contentSearchSortService.parseCursor(cursor, sortBy), idAfter.toString());

    int fetchSize = limit + 1;
    List<MatchedContent> matchedContents = new ArrayList<>(fetchSize);

    long totalCount = 0;
    boolean firstSearch = true;

    while (matchedContents.size() < fetchSize) {
      NativeQueryBuilder queryBuilder = createQueryBuilder(
          request.keywordLike(),
          request.typeEqual(),
          request.tagsIn()
      )
          .withSort(sorts)
          .withPageable(PageRequest.of(0, fetchSize))
          .withTrackTotalHits(firstSearch);

      if (!searchAfter.isEmpty()) {
        queryBuilder.withSearchAfter(searchAfter);
      }

      SearchHits<ContentDocument> searchHits =
          elasticsearchOperations.search(queryBuilder.build(), ContentDocument.class);

      if (firstSearch) {
        totalCount = searchHits.getTotalHits();
        firstSearch = false;
      }

      List<SearchHit<ContentDocument>> hits = searchHits.getSearchHits();

      if (hits.isEmpty()) {
        break;
      }

      List<ContentDocument> documents = hits.stream()
          .map(SearchHit::getContent)
          .toList();

      List<ContentListItemResponse> responses = contentSearchResponseMapper.toResponses(documents);

      Map<UUID, ContentListItemResponse> responsesById = responses.stream()
          .collect(Collectors.toMap(ContentListItemResponse::id, response -> response));

      for (SearchHit<ContentDocument> hit : hits) {
        UUID contentId = UUID.fromString(hit.getContent().getId());
        ContentListItemResponse response = responsesById.get(contentId);

        if (response != null) {
          matchedContents.add(new MatchedContent(hit, response));

          if (matchedContents.size() == fetchSize) {
            break;
          }
        }
      }

      if (matchedContents.size() == fetchSize) {
        break;
      }

      if (hits.size() < fetchSize) {
        break;
      }

      searchAfter = hits.get(hits.size() - 1).getSortValues();
    }

    boolean hasNext = matchedContents.size() > limit;

    List<MatchedContent> pageContents = hasNext
        ? matchedContents.subList(0, limit)
        : matchedContents;

    List<ContentListItemResponse> data = pageContents.stream()
        .map(MatchedContent::response)
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      SearchHit<ContentDocument> lastHit = pageContents.get(pageContents.size() - 1).hit();

      nextCursor = lastHit.getSortValues().get(0).toString();
      nextIdAfter = UUID.fromString(lastHit.getSortValues().get(1).toString());
    }

    ContentCursorResponse response =
        new ContentCursorResponse(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy, sortDirection);

    if (cursor == null && idAfter == null) {
      publishSearchExecuted(request.keywordLike());
    }

    return response;
  }

  private void publishSearchExecuted(String rawKeyword) {
    String keyword = KeywordNormalizer.normalize(rawKeyword);

    if (!KeywordNormalizer.isValidLength(keyword)) {
      return;
    }

    try {
      eventPublisher.publishEvent(new SearchExecutedEvent(keyword));
    } catch (RuntimeException exception) {
      log.warn("검색 집계 이벤트 발행 실패", exception);
    }
  }

  private record MatchedContent(
      SearchHit<ContentDocument> hit,
      ContentListItemResponse response
  ) {
  }

  // 검색어·타입·태그 조건 구성
  private NativeQueryBuilder createQueryBuilder(
      String rawKeyword, String typeEqual, List<String> tagsIn) {
    String keyword = KeywordNormalizer.normalize(rawKeyword);
    ContentType type = toContentType(typeEqual);
    List<String> tags = normalizeTags(tagsIn);

    BoolQuery.Builder boolQuery = new BoolQuery.Builder();

    if (keyword.isBlank()) {
      boolQuery.must(q -> q.matchAll(m -> m));
    } else {
      boolQuery.must(q -> q.multiMatch(m -> m
          .query(keyword).fields("title", "description", "tags")));
    }

    if (type != null) {
      boolQuery.filter(q -> q.term(t -> t
          .field("type").value(type.name())));
    }

    if (!tags.isEmpty()) {
      List<FieldValue> tagValues = tags.stream().map(FieldValue::of).toList();

      boolQuery.filter(q -> q.terms(t -> t
          .field("tags").terms(values -> values.value(tagValues))));
    }

    return NativeQuery.builder().withQuery(q -> q.bool(boolQuery.build()));
  }

  private String resolvePageSortBy(String sortBy) {
    String resolved = trimToNull(sortBy);
    return resolved == null ? DEFAULT_SORT_BY : resolved;
  }

  private int resolveLimit(Integer limit) {
    int resolved = limit == null ? DEFAULT_SEARCH_LIMIT : limit;

    if (resolved < 1 || resolved > MAX_SEARCH_LIMIT) {
      throw new IllegalArgumentException("limit은 1 이상 100 이하여야 합니다.");
    }

    return resolved;
  }

  private ContentType toContentType(String typeEqual) {
    if (typeEqual == null || typeEqual.isBlank()) {
      return null;
    }

    return switch (typeEqual.trim()) {
      case "movie" -> ContentType.MOVIE;
      case "tvSeries" -> ContentType.TV;
      case "sport" -> ContentType.SPORT;
      default -> throw new IllegalArgumentException("typeEqual은 movie, tvSeries, sport 중 하나여야 합니다.");
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
