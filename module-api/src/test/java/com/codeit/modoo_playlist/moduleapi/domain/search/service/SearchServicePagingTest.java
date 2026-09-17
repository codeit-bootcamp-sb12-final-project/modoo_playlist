package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.data.elasticsearch.test.autoconfigure.DataElasticsearchTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataElasticsearchTest(properties = {
    "spring.elasticsearch.uris=http://localhost:9200"
})
@ContextConfiguration(classes = SearchServicePagingTest.TestConfig.class)
class SearchServicePagingTest {

  @Autowired
  private ContentSearchService contentSearchService;

  @Autowired
  private ContentSearchRepository contentSearchRepository;

  @Autowired
  private ElasticsearchOperations elasticsearchOperations;

  @MockitoBean
  private ContentSearchResponseMapper contentSearchResponseMapper;

  @Test
  @DisplayName("추천순에서 커서로 다음 페이지를 조회하면 중복 없이 이동한다")
  void searchRecommendedWithCursor() {
    List<ContentDocument> documents = List.of(
        createDocument(
            "021",
            "cursorcheck cursorcheck cursorcheck",
            "2026-09-13T00:00:01Z"
        ),
        createDocument(
            "022",
            "cursorcheck cursorcheck",
            "2026-09-13T00:00:02Z"
        ),
        createDocument(
            "023",
            "cursorcheck",
            "2026-09-13T00:00:03Z"
        )
    );

    try {
      contentSearchRepository.saveAll(documents);
      refreshIndex();

      when(contentSearchResponseMapper.toResponses(anyList()))
          .thenAnswer(invocation -> {
            List<ContentDocument> source = invocation.getArgument(0);

            return source.stream()
                .map(this::toResponse)
                .toList();
          });

      ContentListRequest firstRequest = new ContentListRequest(
          "movie",
          "cursorcheck",
          List.of(),
          null,
          null,
          2,
          "DESCENDING",
          "recommended"
      );

      ContentCursorResponse firstPage =
          contentSearchService.searchPage(firstRequest);

      ContentListRequest secondRequest = new ContentListRequest(
          "movie",
          "cursorcheck",
          List.of(),
          firstPage.nextCursor(),
          firstPage.nextIdAfter(),
          2,
          "DESCENDING",
          "recommended"
      );

      ContentCursorResponse secondPage =
          contentSearchService.searchPage(secondRequest);

      assertThat(firstPage.data()).hasSize(2);
      assertThat(firstPage.hasNext()).isTrue();
      assertThat(firstPage.nextCursor()).isNotBlank();
      assertThat(firstPage.nextIdAfter()).isNotNull();

      assertThat(secondPage.data()).hasSize(1);
      assertThat(secondPage.hasNext()).isFalse();

      List<UUID> firstPageIds = firstPage.data().stream()
          .map(ContentListItemResponse::id)
          .toList();

      List<UUID> secondPageIds = secondPage.data().stream()
          .map(ContentListItemResponse::id)
          .toList();

      assertThat(firstPageIds)
          .doesNotContainAnyElementsOf(secondPageIds);
    } finally {
      contentSearchRepository.deleteAll(documents);
      refreshIndex();
    }
  }

  private ContentDocument createDocument(
      String suffix,
      String title,
      String createdAt
  ) {
    return ContentDocument.builder()
        .id(id(suffix))
        .type(ContentType.MOVIE)
        .title(title)
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
        document.getType().name(),
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

  private String id(String suffix) {
    return "019ed8a0-0000-7000-9200-000000000" + suffix;
  }

  private void refreshIndex() {
    elasticsearchOperations.indexOps(ContentDocument.class).refresh();
  }

  @Configuration
  @AutoConfigurationPackage
  @EnableElasticsearchRepositories(
      basePackages =
          "com.codeit.modoo_playlist.moduleapi.domain.search.repository"
  )
  @Import({
      ContentSearchService.class,
      ContentSearchSortService.class
  })
  static class TestConfig {
  }
}
