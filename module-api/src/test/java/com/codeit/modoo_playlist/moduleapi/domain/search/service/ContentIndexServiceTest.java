package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentIndexReader;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ReindexStateRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@ExtendWith(MockitoExtension.class)
class ContentIndexServiceTest {

  private static final UUID CONTENT_ID =
      UUID.fromString("019ed8a0-0000-7000-9300-000000000001");

  @Mock
  private ContentIndexReader contentIndexReader;

  @Mock
  private ContentSearchRepository contentSearchRepository;

  @Mock
  private ElasticsearchClient elasticsearchClient;

  @Mock
  private ReindexStateRepository reindexStateRepository;

  @Mock
  private ElasticsearchOperations elasticsearchOperations;

  @InjectMocks
  private ContentIndexService contentIndexService;

  @Test
  @DisplayName("콘텐츠 변경 시 현재 검색 인덱스에 문서를 저장한다")
  void indexContent() {
    ContentDocument document = document(CONTENT_ID);
    when(contentIndexReader.readOne(CONTENT_ID)).thenReturn(Optional.of(document));

    contentIndexService.index(CONTENT_ID);

    verify(reindexStateRepository).recordChangedContent(CONTENT_ID);
    verify(contentSearchRepository).save(document);
    verify(contentSearchRepository, never()).deleteById(any());
  }

  @Test
  @DisplayName("삭제된 콘텐츠는 현재 검색 인덱스에서도 삭제한다")
  void deleteMissingContent() {
    when(contentIndexReader.readOne(CONTENT_ID)).thenReturn(Optional.empty());

    contentIndexService.index(CONTENT_ID);

    verify(reindexStateRepository).recordChangedContent(CONTENT_ID);
    verify(contentSearchRepository).deleteById(CONTENT_ID.toString());
    verify(contentSearchRepository, never()).save(any());
  }

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

    assertThatThrownBy(() -> contentIndexService.updateWatcherCount(CONTENT_ID)).isSameAs(exception);

    verify(contentIndexReader, never()).readOne(any());
    verifyNoInteractions(contentSearchRepository);
  }

  @Test
  @DisplayName("재색인 마무리 시 변경분 반영 후 검증하고 alias를 전환한다")
  void finalizeReindexInOrder() {
    ContentIndexService spyService = org.mockito.Mockito.spy(contentIndexService);
    Runnable applyChangedContents = mock(Runnable.class);

    org.mockito.Mockito.doNothing().when(spyService).refreshAndValidate("contents_v2");
    org.mockito.Mockito.doNothing().when(spyService).switchAlias("contents", "contents_v2");

    spyService.finalizeReindex("contents", "contents_v2", applyChangedContents);

    InOrder inOrder = inOrder(applyChangedContents, spyService);
    inOrder.verify(applyChangedContents).run();
    inOrder.verify(spyService).refreshAndValidate("contents_v2");
    inOrder.verify(spyService).switchAlias("contents", "contents_v2");
  }

  @Test
  @DisplayName("재색인 검증 실패 시 alias를 전환하지 않는다")
  void doNotSwitchAliasWhenValidationFails() {
    ContentIndexService spyService = org.mockito.Mockito.spy(contentIndexService);
    Runnable applyChangedContents = mock(Runnable.class);

    doThrow(new IllegalStateException("검증 실패"))
        .when(spyService).refreshAndValidate("contents_v2");

    assertThatThrownBy(
        () -> spyService.finalizeReindex("contents", "contents_v2", applyChangedContents))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("검증 실패");

    verify(applyChangedContents).run();
    verify(spyService, never()).switchAlias(any(), any());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private ElasticsearchException failUpdate(String errorType) throws IOException {
    ElasticsearchException exception = mock(ElasticsearchException.class);
    ErrorCause error = ErrorCause.of(b -> b.type(errorType).reason("테스트용 오류"));

    when(exception.status()).thenReturn(404);
    when(exception.error()).thenReturn(error);

    doThrow(exception).when(elasticsearchClient).update(any(Function.class), eq(Object.class));

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
