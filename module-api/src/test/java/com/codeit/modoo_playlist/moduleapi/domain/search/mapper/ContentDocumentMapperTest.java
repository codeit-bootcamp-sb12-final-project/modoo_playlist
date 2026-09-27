package com.codeit.modoo_playlist.moduleapi.domain.search.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ContentDocumentMapperTest {

  private static final UUID CONTENT_ID = UUID.fromString("019ed8a0-0000-7000-9300-000000000001");

  private final ContentDocumentMapper mapper = new ContentDocumentMapper();

  @Test
  @DisplayName("콘텐츠를 검색 문서로 변환한다")
  void toDocument() {
    Instant createdAt = Instant.parse("2026-09-23T00:00:00Z");

    Content content = Content.builder()
        .type(ContentType.MOVIE)
        .title("  이누야샤  ")
        .description("검색 테스트 콘텐츠")
        .thumbnailUrl("https://example.com/image.jpg")
        .averageRating(new BigDecimal("4.5"))
        .reviewCount(12)
        .build();

    ReflectionTestUtils.setField(content, "id", CONTENT_ID);
    ReflectionTestUtils.setField(content, "createdAt", createdAt);

    ContentDocument document = mapper.toDocument(content, List.of("애니메이션", "판타지"), 7L);

    assertThat(document.getId()).isEqualTo(CONTENT_ID.toString());
    assertThat(document.getType()).isEqualTo(ContentType.MOVIE);
    assertThat(document.getTitle()).isEqualTo("  이누야샤  ");
    assertThat(document.getNormalizedTitle()).isEqualTo("이누야샤");
    assertThat(document.getDescription()).isEqualTo("검색 테스트 콘텐츠");
    assertThat(document.getThumbnailUrl()).isEqualTo("https://example.com/image.jpg");
    assertThat(document.getTags()).containsExactly("애니메이션", "판타지");
    assertThat(document.getAverageRating()).isEqualTo(4.5);
    assertThat(document.getReviewCount()).isEqualTo(12);
    assertThat(document.getWatcherCount()).isEqualTo(7L);
    assertThat(document.getCreatedAt()).isEqualTo(createdAt);
  }

  @Test
  @DisplayName("콘텐츠가 null이면 예외가 발생한다")
  void rejectNullContent() {
    assertThatThrownBy(() -> mapper.toDocument(null, List.of(), 0L))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("콘텐츠는 필수입니다.");
  }

  @Test
  @DisplayName("저장되지 않은 콘텐츠면 예외가 발생한다")
  void rejectContentWithoutId() {
    Content content = Content.builder()
        .type(ContentType.MOVIE)
        .title("테스트 콘텐츠")
        .build();

    assertThatThrownBy(() -> mapper.toDocument(content, List.of(), 0L))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("저장된 콘텐츠의 ID는 필수입니다.");
  }

  @Test
  @DisplayName("태그명 목록이 null이면 예외가 발생한다")
  void rejectNullTagNames() {
    Content content = Content.builder()
        .type(ContentType.MOVIE)
        .title("테스트 콘텐츠")
        .build();

    ReflectionTestUtils.setField(content, "id", CONTENT_ID);

    assertThatThrownBy(() -> mapper.toDocument(content, null, 0L))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("태그명 목록은 필수입니다.");
  }

  @Test
  @DisplayName("시청자 수가 음수면 예외가 발생한다")
  void rejectNegativeWatcherCount() {
    Content content = Content.builder()
        .type(ContentType.MOVIE)
        .title("테스트 콘텐츠")
        .build();

    ReflectionTestUtils.setField(content, "id", CONTENT_ID);

    assertThatThrownBy(() -> mapper.toDocument(content, List.of(), -1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("시청자 수는 0 이상이어야 합니다.");
  }
}
