package com.codeit.modoo_playlist.modulebatch.embedding.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import com.codeit.modoo_playlist.modulebatch.embedding.persistence.ContentEmbeddingMapper;

@ExtendWith(MockitoExtension.class)
class ContentEmbeddingReaderTest {

  @Mock private ContentEmbeddingMapper mapper;

  @Test
  void 첫_페이지가_비어있으면_null을_반환한다() {
    when(mapper.findContentsNeedingEmbedding(0, 100)).thenReturn(List.of());

    ContentEmbeddingReader reader = new ContentEmbeddingReader(mapper, 100);

    assertThat(reader.read()).isNull();
  }

  @Test
  void 한_페이지_안에서는_추가_조회없이_순서대로_반환한다() {
    when(mapper.findContentsNeedingEmbedding(0, 100)).thenReturn(List.of(
        target("c1"), target("c2")
    ));

    ContentEmbeddingReader reader = new ContentEmbeddingReader(mapper, 100);

    assertThat(reader.read().contentId()).isEqualTo("c1");
    assertThat(reader.read().contentId()).isEqualTo("c2");
    verify(mapper, never()).findContentsNeedingEmbedding(100, 100);
  }

  @Test
  void 페이지가_소진되면_오프셋을_다음_페이지로_증가시켜_이어서_읽는다() {
    when(mapper.findContentsNeedingEmbedding(0, 100)).thenReturn(List.of(target("c1")));
    when(mapper.findContentsNeedingEmbedding(100, 100)).thenReturn(List.of(target("c2")));

    ContentEmbeddingReader reader = new ContentEmbeddingReader(mapper, 100);
    reader.read();

    assertThat(reader.read().contentId()).isEqualTo("c2");
  }

  @Test
  void maxItems에_도달하면_더_읽지_않고_남은_페이지도_소비하지_않는다() {
    when(mapper.findContentsNeedingEmbedding(0, 100)).thenReturn(List.of(target("c1"), target("c2")));

    ContentEmbeddingReader reader = new ContentEmbeddingReader(mapper, 1);

    assertThat(reader.read()).isNotNull();
    assertThat(reader.read()).isNull();
  }

  private ContentEmbeddingTarget target(String contentId) {
    return ContentEmbeddingTarget.builder()
        .contentId(contentId).title("제목").description("설명")
        .thumbnailUrl("https://thumb").tagNames("태그").build();
  }
}
