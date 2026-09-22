package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ReindexStateRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InitialIndexServiceTest {

  private static final String INDEX_ALIAS = "contents";
  private static final String TARGET_INDEX = "contents_v2";
  private static final UUID CONTENT_ID =
      UUID.fromString("019ed8a0-0000-7000-9300-000000000001");

  @Mock
  private ContentIndexService contentIndexService;

  @Mock
  private ReindexStateRepository reindexStateRepository;

  private ContentInitialIndexService contentInitialIndexService;

  @BeforeEach
  void setUp() {
    contentInitialIndexService =
        new ContentInitialIndexService(contentIndexService, reindexStateRepository);
  }

  @Test
  @DisplayName("현재 alias가 최신 인덱스를 가리키면 재색인하지 않는다")
  void skipReindexWhenCurrentIndexIsLatest() {
    when(contentIndexService.getAliasIndex(INDEX_ALIAS))
        .thenReturn(Optional.of(TARGET_INDEX));

    contentInitialIndexService.run(null);

    verify(reindexStateRepository, never()).start(any());
    verify(contentIndexService, never()).createIndex(any());
    verify(contentIndexService, never()).switchAlias(any(), any());
  }

  @Test
  @DisplayName("인덱스 버전이 변경되면 전체 색인 후 alias를 전환한다")
  void reindexAndSwitchAlias() {
    ContentDocument document = ContentDocument.builder()
        .id(CONTENT_ID.toString())
        .build();

    when(contentIndexService.getAliasIndex(INDEX_ALIAS))
        .thenReturn(Optional.empty());
    when(reindexStateRepository.start(TARGET_INDEX))
        .thenReturn(true);
    when(contentIndexService.concreteIndexExists(TARGET_INDEX))
        .thenReturn(false);
    when(contentIndexService.indexBatch(null, 100, TARGET_INDEX))
        .thenReturn(List.of(document));
    when(contentIndexService.indexBatch(CONTENT_ID, 100, TARGET_INDEX))
        .thenReturn(List.of());
    when(reindexStateRepository.popChangedContentId())
        .thenReturn(null);
    when(reindexStateRepository.startSwitching())
        .thenReturn(true);

    contentInitialIndexService.run(null);

    InOrder inOrder = inOrder(contentIndexService, reindexStateRepository);
    inOrder.verify(reindexStateRepository).start(TARGET_INDEX);
    inOrder.verify(contentIndexService).createIndex(TARGET_INDEX);
    inOrder.verify(contentIndexService).indexBatch(null, 100, TARGET_INDEX);
    inOrder.verify(contentIndexService).indexBatch(CONTENT_ID, 100, TARGET_INDEX);
    inOrder.verify(reindexStateRepository).startSwitching();
    inOrder.verify(contentIndexService).finalizeReindex(eq(INDEX_ALIAS), eq(TARGET_INDEX), any(Runnable.class));
    inOrder.verify(reindexStateRepository).finish();
  }

  @Test
  @DisplayName("전체 색인에 실패하면 미완성 인덱스를 삭제하고 재색인 상태를 정리한다")
  void cleanUpWhenReindexFails() {
    when(contentIndexService.getAliasIndex(INDEX_ALIAS))
        .thenReturn(Optional.empty());
    when(reindexStateRepository.start(TARGET_INDEX))
        .thenReturn(true);
    when(contentIndexService.concreteIndexExists(TARGET_INDEX))
        .thenReturn(false);
    when(contentIndexService.indexBatch(null, 100, TARGET_INDEX))
        .thenThrow(new IllegalStateException("색인 실패"));

    assertThatThrownBy(() -> contentInitialIndexService.run(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("색인 실패");

    verify(contentIndexService).deleteIndex(TARGET_INDEX);
    verify(contentIndexService, never()).switchAlias(any(), any());
    verify(reindexStateRepository).finish();
  }
}
