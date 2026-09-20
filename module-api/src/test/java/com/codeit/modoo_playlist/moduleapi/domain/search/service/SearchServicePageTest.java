package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentSearchResponseMapper;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentListItemResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.data.elasticsearch.test.autoconfigure.DataElasticsearchTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public class SearchServicePageTest {

  @Nested
  @DisplayName("검색 집계 이벤트")
  @ExtendWith(MockitoExtension.class)
  class UnitTests {

    @Mock
    private ElasticsearchOperations operations;

    @Mock
    private ContentSearchResponseMapper responseMapper;

    @Mock
    private SearchHits<ContentDocument> searchHits;

    private ContentSearchService service;

    @BeforeEach
    void setUp() {
      service = new ContentSearchService(operations, responseMapper, new ContentSearchSortService());
    }

    private void givenEmptyResults() {
      when(operations.search(any(Query.class), eq(ContentDocument.class))).thenReturn(searchHits);
      when(searchHits.getSearchHits()).thenReturn(List.of());
      when(searchHits.getTotalHits()).thenReturn(0L);
    }

    private ContentListRequest request(String keyword, String cursor, UUID idAfter) {
      return new ContentListRequest(
          null, keyword, null, cursor, idAfter, 20, "DESCENDING", "createdAt"
      );
    }
  }

  @Nested
  @DisplayName("ES 커서 페이징")
  @DataElasticsearchTest(properties = {
      "spring.elasticsearch.uris=http://localhost:9200"
  })
  @ContextConfiguration(classes = SearchServicePageTest.TestConfig.class)
  class IntegrationTests {

    @Autowired
    private ContentSearchService contentSearchService;

    @Autowired
    private ContentSearchRepository contentSearchRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @MockitoBean
    private ContentSearchResponseMapper contentSearchResponseMapper;

    @Test
    void 추천순_다음페이지는_중복없이_조회한다() {
      List<ContentDocument> documents = List.of(
          document("021", "cursorcheck cursorcheck cursorcheck", "2026-09-13T00:00:01Z"),
          document("022", "cursorcheck cursorcheck", "2026-09-13T00:00:02Z"),
          document("023", "cursorcheck", "2026-09-13T00:00:03Z")
      );

      // 기존 문서를 덮어쓰지 않도록 테스트 ID 충돌을 먼저 확인한다.
      for (ContentDocument document : documents) {
        assertThat(contentSearchRepository.existsById(document.getId()))
            .as("테스트 문서 ID가 이미 존재합니다: %s", document.getId())
            .isFalse();
      }

      try {
        contentSearchRepository.saveAll(documents);
        refreshIndex();

        when(contentSearchResponseMapper.toResponses(anyList())).thenAnswer(invocation -> {
          List<ContentDocument> source = invocation.getArgument(0);
          return source.stream().map(this::toResponse).toList();
        });

        ContentCursorResponse firstPage = contentSearchService.searchPage(request(null, null));

        assertThat(firstPage.data()).hasSize(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.nextCursor()).isNotBlank();
        assertThat(firstPage.nextIdAfter()).isNotNull();
        assertThat(firstPage.totalCount()).isEqualTo(3);

        ContentCursorResponse secondPage = contentSearchService.searchPage(
            request(firstPage.nextCursor(), firstPage.nextIdAfter())
        );

        assertThat(secondPage.data()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();

        List<UUID> firstIds = firstPage.data().stream().map(ContentListItemResponse::id).toList();
        List<UUID> secondIds = secondPage.data().stream().map(ContentListItemResponse::id).toList();

        assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);

        List<UUID> expectedIds = documents.stream()
            .map(document -> UUID.fromString(document.getId()))
            .toList();

        List<UUID> actualIds = java.util.stream.Stream.concat(
            firstIds.stream(), secondIds.stream()
        ).toList();

        assertThat(actualIds).containsExactlyInAnyOrderElementsOf(expectedIds);
      } finally {
        contentSearchRepository.deleteAll(documents);
        refreshIndex();
      }
    }

    private ContentListRequest request(String cursor, UUID idAfter) {
      return new ContentListRequest(
          "movie", "cursorcheck", List.of(), cursor, idAfter, 2, "DESCENDING", "recommended"
      );
    }

    private ContentDocument document(String suffix, String title, String createdAt) {
      return ContentDocument.builder()
          .id("019ed8a0-0000-7000-9200-000000000" + suffix)
          .type(ContentType.MOVIE)
          .title(title)
          .normalizedTitle(title)
          .description("Search paging test document")
          .tags(List.of())
          .averageRating(1.0)
          .reviewCount(0)
          .watcherCount(0)
          .createdAt(Instant.parse(createdAt))
          .build();
    }

    private ContentListItemResponse toResponse(ContentDocument document) {
      return new ContentListItemResponse(
          UUID.fromString(document.getId()),
          "movie",
          document.getTitle(),
          document.getDescription(),
          null,
          null,
          document.getTags(),
          BigDecimal.valueOf(document.getAverageRating()),
          document.getReviewCount(),
          document.getWatcherCount()
      );
    }

    private void refreshIndex() {
      elasticsearchOperations.indexOps(ContentDocument.class).refresh();
    }
  }

  @Configuration
  @AutoConfigurationPackage
  @EnableElasticsearchRepositories(
      basePackages = "com.codeit.modoo_playlist.moduleapi.domain.search.repository"
  )
  @Import({ContentSearchService.class, ContentSearchSortService.class})
  static class TestConfig {
  }

}
