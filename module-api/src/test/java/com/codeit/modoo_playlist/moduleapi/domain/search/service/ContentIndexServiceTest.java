package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.RefreshResponse;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentIndexReader;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ReindexStateRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
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
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

@ExtendWith(MockitoExtension.class)
class ContentIndexServiceTest {

  private static final UUID CONTENT_ID = UUID.fromString("019ed8a0-0000-7000-9300-000000000001");

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
  @DisplayName("콘텐츠를 지정한 검색 인덱스에 저장한다")
  void indexContentToTargetIndex() throws IOException {
    ContentDocument document = document(CONTENT_ID);
    when(contentIndexReader.readOne(CONTENT_ID)).thenReturn(Optional.of(document));

    contentIndexService.index(CONTENT_ID, "contents_v2");

    verify(elasticsearchClient).index(any(Function.class));
  }

  @Test
  @DisplayName("삭제된 콘텐츠는 지정한 검색 인덱스에서도 삭제한다")
  void deleteMissingContentFromTargetIndex() throws IOException {
    when(contentIndexReader.readOne(CONTENT_ID)).thenReturn(Optional.empty());

    contentIndexService.index(CONTENT_ID, "contents_v2");

    verify(elasticsearchClient).delete(any(Function.class));
  }

  @Test
  @DisplayName("재색인 전환 중 콘텐츠 변경은 현재 인덱스와 대상 인덱스에 모두 반영한다")
  void indexContentDuringSwitching() throws IOException {
    ContentDocument document = document(CONTENT_ID);

    when(contentIndexReader.readOne(CONTENT_ID)).thenReturn(Optional.of(document));
    when(reindexStateRepository.isSwitching()).thenReturn(true);
    when(reindexStateRepository.getTargetIndex()).thenReturn("contents_v2");

    contentIndexService.index(CONTENT_ID);

    verify(contentSearchRepository).save(document);
    verify(elasticsearchClient).index(any(Function.class));
  }

  @Test
  @DisplayName("재색인 전환 중 시청자 수 변경은 대상 인덱스에도 반영한다")
  void updateWatcherCountDuringSwitching() throws IOException {
    ContentDocument document = document(CONTENT_ID);

    when(contentIndexReader.readWatcherCount(CONTENT_ID)).thenReturn(5L);
    when(reindexStateRepository.isSwitching()).thenReturn(true);
    when(reindexStateRepository.getTargetIndex()).thenReturn("contents_v2");
    when(contentIndexReader.readOne(CONTENT_ID)).thenReturn(Optional.of(document));

    contentIndexService.updateWatcherCount(CONTENT_ID);

    verify(elasticsearchClient).update(any(Function.class), eq(Object.class));
    verify(elasticsearchClient).index(any(Function.class));
  }

  @Test
  @DisplayName("여러 콘텐츠를 지정한 검색 인덱스에 Bulk 색인한다")
  void indexBatchToTargetIndex() throws IOException {
    ContentDocument first = document(CONTENT_ID);
    ContentDocument second = document(
        UUID.fromString("019ed8a0-0000-7000-9300-000000000002"));

    when(contentIndexReader.read(null, 100)).thenReturn(List.of(first, second));

    BulkResponse response = mock(BulkResponse.class);
    when(response.errors()).thenReturn(false);
    when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(response);

    List<ContentDocument> result =
        contentIndexService.indexBatch(null, 100, "contents_v2");

    assertThat(result).containsExactly(first, second);
    verify(elasticsearchClient).bulk(any(BulkRequest.class));
  }

  @Test
  @DisplayName("색인할 콘텐츠가 없으면 Bulk 요청을 보내지 않는다")
  void skipEmptyBatch() throws IOException {
    when(contentIndexReader.read(null, 100)).thenReturn(List.of());

    List<ContentDocument> result = contentIndexService.indexBatch(null, 100, "contents_v2");

    assertThat(result).isEmpty();
    verify(elasticsearchClient, never()).bulk(any(BulkRequest.class));
  }

  @Test
  @DisplayName("Bulk 색인 응답에 오류가 있으면 예외를 발생시킨다")
  void failBulkIndex() throws IOException {
    ContentDocument document = document(CONTENT_ID);
    when(contentIndexReader.read(null, 100)).thenReturn(List.of(document));

    BulkResponse response = mock(BulkResponse.class);
    when(response.errors()).thenReturn(true);
    when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(response);

    assertThatThrownBy(
        () -> contentIndexService.indexBatch(null, 100, "contents_v2"))
        .isInstanceOf(IllegalStateException.class).hasMessage("ES 콘텐츠 Bulk 색인에 실패했습니다. index=contents_v2");
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
  @DisplayName("재색인 대상에 normalizedTitle 누락 문서가 있으면 검증에 실패한다")
  void failValidationWhenNormalizedTitleIsMissing() throws IOException {
    ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
    RefreshResponse refreshResponse = mock(RefreshResponse.class);
    ShardStatistics refreshShards = mock(ShardStatistics.class);
    CountResponse countResponse = mock(CountResponse.class);
    ShardStatistics countShards = mock(ShardStatistics.class);

    when(elasticsearchClient.indices()).thenReturn(indicesClient);

    when(refreshResponse.shards()).thenReturn(refreshShards);
    when(refreshShards.failed()).thenReturn(0);
    when(indicesClient.refresh(any(Function.class))).thenReturn(refreshResponse);

    when(countResponse.shards()).thenReturn(countShards);
    when(countShards.failed()).thenReturn(0);
    when(countResponse.count()).thenReturn(1L);
    when(elasticsearchClient.count(any(Function.class))).thenReturn(countResponse);

    assertThatThrownBy(() -> contentIndexService.refreshAndValidate("contents_v2"))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("normalizedTitle 누락 문서가 있습니다");
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

  @Test
  @DisplayName("재색인 대상 인덱스를 생성한다")
  void createTargetIndex() {
    IndexOperations indexOperations = mock(IndexOperations.class);

    when(elasticsearchOperations.indexOps(IndexCoordinates.of("contents_v2"))).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(false);
    when(indexOperations.createSettings(ContentDocument.class)).thenReturn(null);
    when(indexOperations.createMapping(ContentDocument.class)).thenReturn(null);
    when(indexOperations.create(null, null)).thenReturn(true);

    contentIndexService.createIndex("contents_v2");

    verify(indexOperations).create(null, null);
  }

  @Test
  @DisplayName("이미 존재하는 인덱스는 다시 생성하지 않는다")
  void skipExistingIndexCreation() {
    IndexOperations indexOperations = mock(IndexOperations.class);

    when(elasticsearchOperations.indexOps(IndexCoordinates.of("contents_v2"))).thenReturn(indexOperations);
    when(indexOperations.exists()).thenReturn(true);

    contentIndexService.createIndex("contents_v2");

    verify(indexOperations, never()).create(any(), any());
  }

  @Test
  @DisplayName("현재 alias를 대상 인덱스로 전환한다")
  void switchAliasToTargetIndex() throws IOException {
    ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
    GetAliasResponse currentAlias = mock(GetAliasResponse.class);
    GetAliasResponse switchedAlias = mock(GetAliasResponse.class);
    UpdateAliasesResponse updateResponse = mock(UpdateAliasesResponse.class);

    when(elasticsearchClient.indices()).thenReturn(indicesClient);
    when(currentAlias.aliases()).thenReturn(Collections.singletonMap("contents_v1", null));
    when(switchedAlias.aliases()).thenReturn(Collections.singletonMap("contents_v2", null));
    when(indicesClient.getAlias(any(Function.class))).thenReturn(currentAlias).thenReturn(switchedAlias);
    when(updateResponse.acknowledged()).thenReturn(true);
    when(indicesClient.updateAliases(any(Function.class))).thenReturn(updateResponse);

    contentIndexService.switchAlias("contents", "contents_v2");

    verify(indicesClient).updateAliases(any(Function.class));
  }

  @Test
  @DisplayName("존재하는 인덱스를 삭제한다")
  void deleteExistingIndex() throws IOException {
    ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
    GetAliasResponse aliasResponse = mock(GetAliasResponse.class);

    when(elasticsearchClient.indices()).thenReturn(indicesClient);
    when(aliasResponse.aliases()).thenReturn(Collections.emptyMap());
    when(indicesClient.getAlias(any(Function.class))).thenReturn(aliasResponse);
    when(indicesClient.exists(any(Function.class))).thenReturn(new BooleanResponse(true));
    contentIndexService.deleteIndex("contents_v2");

    verify(indicesClient).delete(any(Function.class));
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
