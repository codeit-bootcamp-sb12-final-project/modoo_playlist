package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentDocumentMapper;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentIndexReader;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

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
class ContentIndexServiceTest {

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
    assertThat(contentRepository.findByIdAndDeletedAtIsNull(TEST_CONTENT_ID))
        .as("로컬 MySQL에 테스트 콘텐츠가 있어야 합니다.")
        .isPresent();

    contentIndexService.index(TEST_CONTENT_ID);

    ContentDocument document = contentSearchRepository
        .findById(TEST_CONTENT_ID.toString())
        .orElseThrow(() -> new AssertionError("ES에 문서가 저장되지 않았습니다."));

    assertThat(document.getId()).isEqualTo(TEST_CONTENT_ID.toString());
    assertThat(document.getType().name()).isEqualTo("MOVIE");
    assertThat(document.getTitle()).isEqualTo("Index Test Movie");
    assertThat(document.getDescription())
        .isEqualTo("A story about people exploring space");
    assertThat(document.getTags()).isEmpty();
    assertThat(document.getAverageRating()).isZero();
    assertThat(document.getReviewCount()).isZero();
    assertThat(document.getWatcherCount())
        .isEqualTo(contentRepository.countCurrentWatchers(TEST_CONTENT_ID));
  }

  @Test
  @DisplayName("초기 전체 색인을 실행하면 DB 콘텐츠와 시청자 수가 ES에 저장된다")
  void indexAllContents() {
    assertThat(contentRepository.findByIdAndDeletedAtIsNull(TEST_CONTENT_ID))
        .as("로컬 MySQL에 테스트 콘텐츠가 있어야 합니다.")
        .isPresent();

    contentSearchRepository.deleteById(TEST_CONTENT_ID.toString());

    assertThat(contentSearchRepository.findById(TEST_CONTENT_ID.toString()))
        .isEmpty();

    long indexedCount = contentInitialIndexService.indexAll();

    assertThat(indexedCount).isPositive();

    ContentDocument document = contentSearchRepository
        .findById(TEST_CONTENT_ID.toString())
        .orElseThrow(() -> new AssertionError("전체 색인 후 ES 문서가 없습니다."));

    assertThat(document.getId()).isEqualTo(TEST_CONTENT_ID.toString());
    assertThat(document.getTitle()).isEqualTo("Index Test Movie");
    assertThat(document.getWatcherCount())
        .isEqualTo(contentRepository.countCurrentWatchers(TEST_CONTENT_ID));
  }

  @Test
  @DisplayName("DB의 현재 시청자 수를 ES에 반영하고 다른 필드는 유지한다")
  void updateWatcherCount() {
    assertThat(contentRepository.findByIdAndDeletedAtIsNull(TEST_CONTENT_ID))
        .as("로컬 MySQL에 테스트 콘텐츠가 있어야 합니다.")
        .isPresent();

    long expectedWatcherCount =
        contentRepository.countCurrentWatchers(TEST_CONTENT_ID);

    var originalDocument =
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

      ContentDocument updatedDocument = contentSearchRepository
          .findById(TEST_CONTENT_ID.toString())
          .orElseThrow();

      assertThat(updatedDocument.getWatcherCount())
          .isEqualTo(expectedWatcherCount);
      assertThat(updatedDocument.getTitle())
          .isEqualTo(testDocument.getTitle());
      assertThat(updatedDocument.getDescription())
          .isEqualTo(testDocument.getDescription());
    } finally {
      if (originalDocument.isPresent()) {
        contentSearchRepository.save(originalDocument.get());
      } else {
        contentSearchRepository.deleteById(TEST_CONTENT_ID.toString());
      }
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
  static class TestConfig {
  }
}
