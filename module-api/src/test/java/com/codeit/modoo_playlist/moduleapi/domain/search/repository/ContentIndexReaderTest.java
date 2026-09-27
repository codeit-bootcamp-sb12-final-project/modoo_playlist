package com.codeit.modoo_playlist.moduleapi.domain.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.mapper.ContentDocumentMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentIndexReaderTest {

  private static final UUID CONTENT_ID = UUID.fromString("019ed8a0-0000-7000-9300-000000000001");

  @Mock
  private EntityManager entityManager;

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private ContentTagRepository contentTagRepository;

  @Mock
  private ContentDocumentMapper contentDocumentMapper;

  @InjectMocks
  private ContentIndexReader contentIndexReader;

  @Test
  @DisplayName("콘텐츠가 없으면 빈 결과를 반환한다")
  void readOneReturnsEmptyWhenContentDoesNotExist() {
    when(contentRepository.findByIdAndDeletedAtIsNull(CONTENT_ID)).thenReturn(Optional.empty());

    Optional<ContentDocument> result = contentIndexReader.readOne(CONTENT_ID);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("조회 개수가 0 이하면 예외가 발생한다")
  void rejectInvalidBatchSize() {
    assertThatThrownBy(() -> contentIndexReader.read(null, 0))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("조회 개수는 1 이상이어야 합니다.");
  }

  @Test
  @DisplayName("조회된 콘텐츠가 없으면 빈 목록을 반환한다")
  void readReturnsEmptyWhenNoContentsExist() {
    @SuppressWarnings("unchecked")
    TypedQuery<Content> query = mock(TypedQuery.class);

    when(entityManager.createQuery(
        org.mockito.ArgumentMatchers.anyString(), eq(Content.class))).thenReturn(query);
    when(query.setMaxResults(100)).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of());

    List<ContentDocument> result = contentIndexReader.read(null, 100);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("시청 중인 사용자가 없으면 시청자 수 0을 반환한다")
  void readWatcherCountReturnsZero() {
    @SuppressWarnings("unchecked")
    TypedQuery<Tuple> query = mock(TypedQuery.class);

    when(entityManager.createQuery(
        org.mockito.ArgumentMatchers.anyString(), eq(Tuple.class))).thenReturn(query);
    when(query.setParameter("contentIds", List.of(CONTENT_ID))).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of());

    long watcherCount = contentIndexReader.readWatcherCount(CONTENT_ID);

    assertThat(watcherCount).isZero();
  }

  @Test
  @DisplayName("시청 중인 사용자 수를 반환한다")
  void readWatcherCount() {
    @SuppressWarnings("unchecked")
    TypedQuery<Tuple> query = mock(TypedQuery.class);
    Tuple row = mock(Tuple.class);

    when(entityManager.createQuery(
        org.mockito.ArgumentMatchers.anyString(), eq(Tuple.class))).thenReturn(query);
    when(query.setParameter("contentIds", List.of(CONTENT_ID))).thenReturn(query);
    when(query.getResultList()).thenReturn(List.of(row));
    when(row.get(0, UUID.class)).thenReturn(CONTENT_ID);
    when(row.get(1, Long.class)).thenReturn(3L);

    long watcherCount = contentIndexReader.readWatcherCount(CONTENT_ID);

    assertThat(watcherCount).isEqualTo(3L);
  }

  @Test
  @DisplayName("콘텐츠 한 건을 태그와 시청자 수를 포함해 색인 문서로 변환한다")
  void readOne() {
    Content content = Content.builder()
        .id(CONTENT_ID)
        .type(ContentType.MOVIE)
        .title("이누야샤")
        .averageRating(BigDecimal.ZERO)
        .build();

    UUID tagId = UUID.randomUUID();
    Tag tag = Tag.builder()
        .id(tagId)
        .name("애니메이션")
        .build();

    ContentTag contentTag = ContentTag.builder()
        .id(new ContentTagId(CONTENT_ID, tagId))
        .content(content)
        .tag(tag)
        .build();

    ContentDocument document = mock(ContentDocument.class);

    @SuppressWarnings("unchecked")
    TypedQuery<Tuple> watcherQuery = mock(TypedQuery.class);
    Tuple watcherRow = mock(Tuple.class);

    when(contentRepository.findByIdAndDeletedAtIsNull(CONTENT_ID)).thenReturn(Optional.of(content));
    when(contentTagRepository.findAllWithTagByContentIds(List.of(CONTENT_ID))).thenReturn(List.of(contentTag));

    when(entityManager.createQuery(anyString(), eq(Tuple.class))).thenReturn(watcherQuery);
    when(watcherQuery.setParameter("contentIds", List.of(CONTENT_ID))).thenReturn(watcherQuery);
    when(watcherQuery.getResultList()).thenReturn(List.of(watcherRow));
    when(watcherRow.get(0, UUID.class)).thenReturn(CONTENT_ID);
    when(watcherRow.get(1, Long.class)).thenReturn(3L);

    when(contentDocumentMapper.toDocument(content, List.of("애니메이션"), 3L)).thenReturn(document);

    Optional<ContentDocument> result = contentIndexReader.readOne(CONTENT_ID);

    assertThat(result).contains(document);
    verify(contentDocumentMapper).toDocument(content, List.of("애니메이션"), 3L);
  }

  @Test
  @DisplayName("콘텐츠 목록을 태그와 시청자 수를 포함해 색인 문서 목록으로 변환한다")
  void read() {
    UUID secondContentId = UUID.randomUUID();

    Content firstContent = Content.builder()
        .id(CONTENT_ID)
        .type(ContentType.MOVIE)
        .title("이누야샤")
        .averageRating(BigDecimal.ZERO)
        .build();

    Content secondContent = Content.builder()
        .id(secondContentId)
        .type(ContentType.MOVIE)
        .title("인터스텔라")
        .averageRating(BigDecimal.ZERO)
        .build();

    UUID tagId = UUID.randomUUID();
    Tag tag = Tag.builder()
        .id(tagId)
        .name("판타지")
        .build();

    ContentTag contentTag = ContentTag.builder()
        .id(new ContentTagId(CONTENT_ID, tagId))
        .content(firstContent)
        .tag(tag)
        .build();

    ContentDocument firstDocument = mock(ContentDocument.class);
    ContentDocument secondDocument = mock(ContentDocument.class);

    @SuppressWarnings("unchecked")
    TypedQuery<Content> contentQuery = mock(TypedQuery.class);

    @SuppressWarnings("unchecked")
    TypedQuery<Tuple> watcherQuery = mock(TypedQuery.class);

    Tuple watcherRow = mock(Tuple.class);

    when(entityManager.createQuery(anyString(), eq(Content.class))).thenReturn(contentQuery);
    when(contentQuery.setMaxResults(100)).thenReturn(contentQuery);
    when(contentQuery.getResultList()).thenReturn(List.of(firstContent, secondContent));

    List<UUID> contentIds = List.of(CONTENT_ID, secondContentId);
    when(contentTagRepository.findAllWithTagByContentIds(contentIds)).thenReturn(List.of(contentTag));

    when(entityManager.createQuery(anyString(), eq(Tuple.class))).thenReturn(watcherQuery);
    when(watcherQuery.setParameter("contentIds", contentIds)).thenReturn(watcherQuery);
    when(watcherQuery.getResultList()).thenReturn(List.of(watcherRow));
    when(watcherRow.get(0, UUID.class)).thenReturn(CONTENT_ID);
    when(watcherRow.get(1, Long.class)).thenReturn(5L);

    when(contentDocumentMapper.toDocument(firstContent, List.of("판타지"), 5L)).thenReturn(firstDocument);
    when(contentDocumentMapper.toDocument(secondContent, List.of(), 0L)).thenReturn(secondDocument);

    List<ContentDocument> result = contentIndexReader.read(null, 100);

    assertThat(result).containsExactly(firstDocument, secondDocument);

    verify(contentDocumentMapper).toDocument(firstContent, List.of("판타지"), 5L);
    verify(contentDocumentMapper).toDocument(secondContent, List.of(), 0L);
  }
}
