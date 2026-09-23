package com.codeit.modoo_playlist.moduleapi.domain.search.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentListItemResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentSearchResponseMapperTest {

  private static final UUID FIRST_ID = UUID.fromString("019ed8a0-0000-7000-9300-000000000001");
  private static final UUID SECOND_ID = UUID.fromString("019ed8a0-0000-7000-9300-000000000002");

  @Mock
  private EntityManager entityManager;

  @Mock
  private TypedQuery<Tuple> query;

  private ContentSearchResponseMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ContentSearchResponseMapper(entityManager);
  }

  @Test
  @DisplayName("검색 문서를 기존 순서대로 응답으로 변환한다")
  void toResponses() {
    ContentDocument first = document(FIRST_ID, ContentType.MOVIE, "첫 번째 콘텐츠");
    ContentDocument second = document(SECOND_ID, ContentType.TV, "두 번째 콘텐츠");

    Tuple firstRow = row(FIRST_ID, LocalDate.of(2026, 1, 1), 5L);
    Tuple secondRow = row(SECOND_ID, LocalDate.of(2026, 2, 1), 3L);

    when(entityManager.createQuery(anyString(), eq(Tuple.class))).thenReturn(query);
    when(query.setParameter("contentIds", List.of(FIRST_ID, SECOND_ID))).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of(secondRow, firstRow));

    List<ContentListItemResponse> responses = mapper.toResponses(List.of(first, second));

    assertThat(responses).extracting(ContentListItemResponse::id).containsExactly(FIRST_ID, SECOND_ID);
    assertThat(responses).extracting(ContentListItemResponse::type).containsExactly("movie", "tvSeries");
    assertThat(responses.get(0).releaseDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(responses.get(0).watcherCount()).isEqualTo(5L);
    assertThat(responses.get(1).watcherCount()).isEqualTo(3L);
  }

  @Test
  @DisplayName("DB에 없는 콘텐츠는 검색 응답에서 제외한다")
  void excludeMissingContent() {
    ContentDocument first = document(FIRST_ID, ContentType.MOVIE, "존재하는 콘텐츠");
    ContentDocument second = document(SECOND_ID, ContentType.TV, "삭제된 콘텐츠");

    Tuple firstRow = row(FIRST_ID, LocalDate.of(2026, 1, 1), 5L);

    when(entityManager.createQuery(anyString(), eq(Tuple.class))).thenReturn(query);
    when(query.setParameter("contentIds", List.of(FIRST_ID, SECOND_ID))).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of(firstRow));

    List<ContentListItemResponse> responses = mapper.toResponses(List.of(first, second));

    assertThat(responses).hasSize(1);
    assertThat(responses.get(0).id()).isEqualTo(FIRST_ID);
  }

  @Test
  @DisplayName("SPORT 콘텐츠 타입을 sport로 변환한다")
  void convertSportType() {
    ContentDocument document = document(FIRST_ID, ContentType.SPORT, "스포츠 콘텐츠");
    Tuple row = row(FIRST_ID, LocalDate.of(2026, 3, 1), 10L);

    when(entityManager.createQuery(anyString(), eq(Tuple.class))).thenReturn(query);
    when(query.setParameter("contentIds", List.of(FIRST_ID))).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of(row));

    List<ContentListItemResponse> responses = mapper.toResponses(List.of(document));

    assertThat(responses).singleElement().extracting(ContentListItemResponse::type).isEqualTo("sport");
  }

  @Test
  @DisplayName("검색 문서가 없으면 DB를 조회하지 않고 빈 목록을 반환한다")
  void returnEmptyResponses() {
    List<ContentListItemResponse> responses = mapper.toResponses(List.of());

    assertThat(responses).isEmpty();
    verifyNoInteractions(entityManager);
  }

  private ContentDocument document(UUID id, ContentType type, String title) {
    return ContentDocument.builder()
        .id(id.toString())
        .type(type)
        .title(title)
        .description("검색 테스트 콘텐츠")
        .thumbnailUrl("https://example.com/image.jpg")
        .tags(List.of("테스트"))
        .averageRating(4.5)
        .reviewCount(2)
        .watcherCount(0)
        .build();
  }

  private Tuple row(UUID id, LocalDate releaseDate, long watcherCount) {
    Tuple tuple = mock(Tuple.class);

    when(tuple.get(0, UUID.class)).thenReturn(id);
    when(tuple.get(1, LocalDate.class)).thenReturn(releaseDate);
    when(tuple.get(2, Long.class)).thenReturn(watcherCount);

    return tuple;
  }
}
