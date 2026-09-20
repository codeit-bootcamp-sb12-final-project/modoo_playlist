package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentDocumentMapper;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentIndexReader;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.elasticsearch.test.autoconfigure.DataElasticsearchTest;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

class ContentIndexServiceTest {

  @Nested
  @DisplayName("Mock 기반 색인 테스트")
  @ExtendWith(MockitoExtension.class)
  class UnitTests {

    private static final UUID CONTENT_ID =
        UUID.fromString("019ed8a0-0000-7000-9300-000000000001");

    @Mock
    private ContentIndexReader contentIndexReader;

    @Mock
    private ContentSearchRepository contentSearchRepository;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private ContentIndexService contentIndexService;

    @Test
    @DisplayName("누락된 ES 문서를 DB 데이터로 복구한다")
    void recoverMissingDocument() throws IOException {
      ContentDocument document = document(CONTENT_ID);

      when(contentIndexReader.readWatcherCount(CONTENT_ID)).thenReturn(3L);
      when(contentIndexReader.readOne(CONTENT_ID)).thenReturn(Optional.of(document));
      failUpdate("document_missing_exception");

      contentIndexService.updateWatcherCount(CONTENT_ID);

      verify(contentIndexReader).readOne(CONTENT_ID);
      verify(contentSearchRepository).save(document);
      verify(contentSearchRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("활성 콘텐츠가 없으면 문서를 생성하지 않는다")
    void skipMissingContent() throws IOException {
      when(contentIndexReader.readWatcherCount(CONTENT_ID)).thenReturn(0L);
      when(contentIndexReader.readOne(CONTENT_ID)).thenReturn(Optional.empty());
      failUpdate("document_missing_exception");

      contentIndexService.updateWatcherCount(CONTENT_ID);

      verify(contentSearchRepository).deleteById(CONTENT_ID.toString());
      verify(contentSearchRepository, never()).save(any(ContentDocument.class));
    }

    @Test
    @DisplayName("인덱스 누락 오류는 그대로 전달한다")
    void propagateIndexError() throws IOException {
      when(contentIndexReader.readWatcherCount(CONTENT_ID)).thenReturn(3L);
      ElasticsearchException exception = failUpdate("index_not_found_exception");

      assertThatThrownBy(() -> contentIndexService.updateWatcherCount(CONTENT_ID))
          .isSameAs(exception);

      verify(contentIndexReader, never()).readOne(any());
      verifyNoInteractions(contentSearchRepository);
    }

    @Test
    @DisplayName("배치를 한 번씩 저장하고 마지막 ID로 이어서 조회한다")
    void indexBatchesOnce() {
      ContentInitialIndexService initialIndexService =
          new ContentInitialIndexService(contentIndexService, elasticsearchOperations);

      UUID secondId = UUID.fromString("019ed8a0-0000-7000-9300-000000000002");
      UUID thirdId = UUID.fromString("019ed8a0-0000-7000-9300-000000000003");

      List<ContentDocument> firstBatch = List.of(document(CONTENT_ID), document(secondId));
      List<ContentDocument> secondBatch = List.of(document(thirdId));

      when(contentIndexReader.read(null, 100)).thenReturn(firstBatch);
      when(contentIndexReader.read(secondId, 100)).thenReturn(secondBatch);
      when(contentIndexReader.read(thirdId, 100)).thenReturn(List.of());

      long indexedCount = initialIndexService.indexAll();

      assertThat(indexedCount).isEqualTo(3);

      verify(contentSearchRepository).saveAll(firstBatch);
      verify(contentSearchRepository).saveAll(secondBatch);
      verifyNoMoreInteractions(contentSearchRepository);

      verify(contentIndexReader).read(null, 100);
      verify(contentIndexReader).read(secondId, 100);
      verify(contentIndexReader).read(thirdId, 100);
      verifyNoMoreInteractions(contentIndexReader);

      verifyNoInteractions(elasticsearchClient);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ElasticsearchException failUpdate(String errorType) throws IOException {
      ElasticsearchException exception = mock(ElasticsearchException.class);
      ErrorCause error = ErrorCause.of(b -> b.type(errorType).reason("테스트용 오류"));

      when(exception.status()).thenReturn(404);
      when(exception.error()).thenReturn(error);

      doThrow(exception).when(elasticsearchClient)
          .update(any(Function.class), eq(Object.class));

      return exception;
    }

    private ContentDocument document(UUID contentId) {
      return ContentDocument.builder()
          .id(contentId.toString())
          .type(ContentType.MOVIE)
          .title("색인 테스트 콘텐츠")
          .description("DB에서 읽은 전체 문서")
          .tags(List.of("SF"))
          .averageRating(4.5)
          .reviewCount(2)
          .watcherCount(3)
          .createdAt(Instant.parse("2026-09-16T00:00:00Z"))
          .build();
    }
  }

  @Nested
  @DisplayName("DB·ES 통합 테스트")
  @DataElasticsearchTest(properties = {
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.elasticsearch.uris=http://localhost:9200",
      "spring.datasource.url=${DB_URL}",
      "spring.datasource.username=${DB_USERNAME}",
      "spring.datasource.password=${DB_PASSWORD}"
  })
  @ContextConfiguration(classes = ContentIndexServiceTest.TestConfig.class)
  @ImportAutoConfiguration({
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
      TransactionAutoConfiguration.class
  })
  class IntegrationTests {

    private static final UUID TEST_CONTENT_ID =
        UUID.fromString("019ed8a0-0000-7000-8000-000000000002");

    @Autowired
    private ContentIndexService contentIndexService;

    @Autowired
    private ContentInitialIndexService contentInitialIndexService;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private ContentSearchRepository contentSearchRepository;

    @Test
    @DisplayName("DB 콘텐츠를 색인하면 같은 ID의 ES 문서가 저장된다")
    void indexContent() {
      assertTestContentExists();

      contentIndexService.index(TEST_CONTENT_ID);

      ContentDocument document = findIndexedDocument();

      assertThat(document.getId()).isEqualTo(TEST_CONTENT_ID.toString());
      assertThat(document.getType().name()).isEqualTo("MOVIE");
      assertThat(document.getTitle()).isEqualTo("Index Test Movie");
      assertThat(document.getDescription()).isEqualTo("A story about people exploring space");
      assertThat(document.getTags()).isEmpty();
      assertThat(document.getAverageRating()).isZero();
      assertThat(document.getReviewCount()).isZero();
      assertThat(document.getWatcherCount())
          .isEqualTo(contentRepository.countCurrentWatchers(TEST_CONTENT_ID));
    }

    @Test
    @DisplayName("초기 전체 색인으로 DB 콘텐츠와 시청자 수를 ES에 저장한다")
    void indexAllContents() {
      assertTestContentExists();

      contentSearchRepository.deleteById(TEST_CONTENT_ID.toString());
      assertThat(contentSearchRepository.findById(TEST_CONTENT_ID.toString())).isEmpty();

      long indexedCount = contentInitialIndexService.indexAll();

      assertThat(indexedCount).isPositive();

      ContentDocument document = findIndexedDocument();

      assertThat(document.getId()).isEqualTo(TEST_CONTENT_ID.toString());
      assertThat(document.getTitle()).isEqualTo("Index Test Movie");
      assertThat(document.getWatcherCount())
          .isEqualTo(contentRepository.countCurrentWatchers(TEST_CONTENT_ID));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "MODOO_REINDEX", matches = "true")
    @DisplayName("로컬 콘텐츠를 재색인한다")
    void reindexLocalContents() {
      long indexedCount = contentInitialIndexService.indexAll();

      System.out.println("재색인한 콘텐츠 수: " + indexedCount);
      assertThat(indexedCount).isPositive();
    }

    @Test
    @DisplayName("현재 시청자 수를 반영하고 다른 필드는 유지한다")
    void updateWatcherCount() {
      assertTestContentExists();

      long expectedWatcherCount = contentRepository.countCurrentWatchers(TEST_CONTENT_ID);
      Optional<ContentDocument> original =
          contentSearchRepository.findById(TEST_CONTENT_ID.toString());

      ContentDocument testDocument = ContentDocument.builder()
          .id(TEST_CONTENT_ID.toString())
          .title("시청자 수 부분 업데이트 테스트")
          .description("이 설명은 변경되면 안 됩니다.")
          .watcherCount(expectedWatcherCount + 1)
          .build();

      try {
        contentSearchRepository.save(testDocument);

        contentIndexService.updateWatcherCount(TEST_CONTENT_ID);

        ContentDocument updated = findIndexedDocument();

        assertThat(updated.getWatcherCount()).isEqualTo(expectedWatcherCount);
        assertThat(updated.getTitle()).isEqualTo(testDocument.getTitle());
        assertThat(updated.getDescription()).isEqualTo(testDocument.getDescription());
      } finally {
        if (original.isPresent()) {
          contentSearchRepository.save(original.get());
        } else {
          contentSearchRepository.deleteById(TEST_CONTENT_ID.toString());
        }
      }
    }

    private void assertTestContentExists() {
      assertThat(contentRepository.findByIdAndDeletedAtIsNull(TEST_CONTENT_ID))
          .as("로컬 MySQL에 기존 테스트 콘텐츠가 있어야 합니다.")
          .isPresent();
    }

    private ContentDocument findIndexedDocument() {
      return contentSearchRepository.findById(TEST_CONTENT_ID.toString())
          .orElseThrow(() -> new AssertionError("ES에 테스트 콘텐츠 문서가 없습니다."));
    }
  }

  @Configuration
  @AutoConfigurationPackage
  @EntityScan("com.codeit.modoo_playlist.core")
  @EnableJpaRepositories(
      basePackages = "com.codeit.modoo_playlist.moduleapi.domain.content.repository"
  )
  @EnableElasticsearchRepositories(
      basePackages = "com.codeit.modoo_playlist.moduleapi.domain.search.repository"
  )
  @PropertySource("file:../.env")
  @Import({
      ContentIndexService.class,
      ContentInitialIndexService.class,
      ContentIndexReader.class,
      ContentDocumentMapper.class
  })
  static class TestConfig {}
}