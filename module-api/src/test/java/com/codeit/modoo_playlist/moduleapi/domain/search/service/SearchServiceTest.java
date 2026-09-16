package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.data.elasticsearch.test.autoconfigure.DataElasticsearchTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataElasticsearchTest(properties = {
    "spring.elasticsearch.uris=http://localhost:9200"
})
@ContextConfiguration(classes = SearchServiceTest.TestConfig.class)
class SearchServiceTest {

  private static final String TEST_CONTENT_ID =
      "019ed8a0-0000-7000-8000-000000000001";

  @Autowired
  private ContentSearchService contentSearchService;

  @Autowired
  private ContentSearchRepository contentSearchRepository;

  @Autowired
  private ElasticsearchOperations elasticsearchOperations;

  @MockitoBean
  private com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentSearchResponseMapper
      contentSearchResponseMapper;

  @Test
  @DisplayName("제목에 포함된 검색어로 콘텐츠를 검색한다")
  void searchByTitle() {
    SearchHits<ContentDocument> results =
        contentSearchService.search("인터스텔라");

    assertThat(results)
        .anySatisfy(hit -> {
          assertThat(hit.getId()).isEqualTo(TEST_CONTENT_ID);
          assertThat(hit.getContent().getTitle()).isEqualTo("인터스텔라");
        });
  }

  @Test
  @DisplayName("설명에 포함된 검색어로 콘텐츠를 검색한다")
  void searchByDescription() {
    SearchHits<ContentDocument> results =
        contentSearchService.search("우주");

    assertThat(results)
        .anySatisfy(hit -> {
          assertThat(hit.getId()).isEqualTo(TEST_CONTENT_ID);
          assertThat(hit.getContent().getTitle()).isEqualTo("인터스텔라");
        });
  }

  @Test
  @DisplayName("검색어·타입·태그 조건을 함께 적용한다")
  void searchWithFilters() {
    String keyword = "filtercheck";

    ContentDocument sfMovie =
        createDocument(
            "001",
            ContentType.MOVIE,
            keyword,
            List.of("SF"),
            1.0,
            "2026-09-13T00:00:00Z"
        );

    ContentDocument adventureMovie =
        createDocument(
            "002",
            ContentType.MOVIE,
            keyword,
            List.of("모험"),
            2.0,
            "2026-09-13T00:00:01Z"
        );

    ContentDocument sfTv =
        createDocument(
            "003",
            ContentType.TV,
            keyword,
            List.of("SF"),
            3.0,
            "2026-09-13T00:00:02Z"
        );

    ContentDocument comedyMovie =
        createDocument(
            "004",
            ContentType.MOVIE,
            keyword,
            List.of("코미디"),
            4.0,
            "2026-09-13T00:00:03Z"
        );

    List<ContentDocument> documents =
        List.of(sfMovie, adventureMovie, sfTv, comedyMovie);

    try {
      contentSearchRepository.saveAll(documents);
      refreshIndex();

      SearchHits<ContentDocument> result =
          contentSearchService.search(
              keyword,
              "movie",
              List.of("SF", "모험"),
              20
          );

      assertThat(result.getSearchHits())
          .extracting(hit -> hit.getContent().getId())
          .containsExactlyInAnyOrder(
              sfMovie.getId(),
              adventureMovie.getId()
          );
    } finally {
      deleteDocuments(documents);
    }
  }

  @ParameterizedTest
  @CsvSource({
      "createdAt, DESCENDING, 003, 002, 001",
      "createdAt, ASCENDING, 001, 002, 003",
      "rate, DESCENDING, 003, 002, 001",
      "rate, ASCENDING, 001, 002, 003"
  })
  @DisplayName("생성 시각과 평점으로 콘텐츠를 정렬한다")
  void searchWithSort(
      String sortBy,
      String sortDirection,
      String first,
      String second,
      String third
  ) {
    List<ContentDocument> documents = List.of(
        createDocument(
            "001",
            ContentType.MOVIE,
            "sortcheck",
            List.of(),
            1.0,
            "2026-09-13T00:00:01Z"
        ),
        createDocument(
            "002",
            ContentType.MOVIE,
            "sortcheck",
            List.of(),
            2.0,
            "2026-09-13T00:00:02Z"
        ),
        createDocument(
            "003",
            ContentType.MOVIE,
            "sortcheck",
            List.of(),
            3.0,
            "2026-09-13T00:00:03Z"
        )
    );

    try {
      contentSearchRepository.saveAll(documents);
      refreshIndex();

      SearchHits<ContentDocument> result =
          contentSearchService.search(
              "sortcheck",
              "movie",
              List.of(),
              20,
              sortBy,
              sortDirection
          );

      assertThat(result.getSearchHits())
          .extracting(hit -> hit.getContent().getId())
          .containsExactly(
              id(first),
              id(second),
              id(third)
          );
    } finally {
      deleteDocuments(documents);
    }
  }

  @Test
  @DisplayName("추천순은 검색 관련도 순으로 콘텐츠를 정렬한다")
  void searchByRecommended() {
    ContentDocument highScoreDocument =
        createDocument(
            "011",
            ContentType.MOVIE,
            "추천테스트 추천테스트 추천테스트",
            List.of(),
            1.0,
            "2026-09-13T00:00:01Z"
        );

    ContentDocument lowScoreDocument = ContentDocument.builder()
        .id(id("012"))
        .type(ContentType.MOVIE)
        .title("일반 영화")
        .description("추천테스트")
        .tags(List.of())
        .averageRating(1.0)
        .reviewCount(0)
        .watcherCount(0)
        .createdAt(Instant.parse("2026-09-13T00:00:02Z"))
        .build();

    List<ContentDocument> documents =
        List.of(highScoreDocument, lowScoreDocument);

    try {
      contentSearchRepository.saveAll(documents);
      refreshIndex();

      SearchHits<ContentDocument> result =
          contentSearchService.search(
              "추천테스트",
              "movie",
              List.of(),
              20,
              "recommended",
              "DESCENDING"
          );

      assertThat(result.getSearchHits())
          .extracting(hit -> hit.getContent().getId())
          .containsExactly(
              highScoreDocument.getId(),
              lowScoreDocument.getId()
          );
    } finally {
      deleteDocuments(documents);
    }
  }

  private ContentDocument createDocument(
      String suffix,
      ContentType type,
      String keyword,
      List<String> tags,
      double averageRating,
      String createdAt
  ) {
    return ContentDocument.builder()
        .id(id(suffix))
        .type(type)
        .title(keyword)
        .description("Search service test document")
        .tags(tags)
        .averageRating(averageRating)
        .reviewCount(0)
        .watcherCount(0)
        .createdAt(Instant.parse(createdAt))
        .build();
  }

  private String id(String suffix) {
    return "019ed8a0-0000-7000-9200-000000000" + suffix;
  }

  private void deleteDocuments(List<ContentDocument> documents) {
    contentSearchRepository.deleteAll(documents);
    refreshIndex();
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
