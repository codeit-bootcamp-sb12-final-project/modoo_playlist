package com.codeit.modoo_playlist.modulebatch.embedding.processor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingResult;
import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;

class ContentEmbeddingProcessorTest {

  private static final String MODEL = "gemini-embedding-2";

  private final ContentEmbeddingProcessor processor = new ContentEmbeddingProcessor(MODEL);

  @Test
  void 해시가_기존과_같으면_변경없음으로_보고_null을_반환한다() {
    ContentEmbeddingTarget withoutHash = target(null);
    String currentHash = expectedHash(withoutHash);
    ContentEmbeddingTarget target = target(currentHash);

    assertThat(processor.process(target)).isNull();
  }

  @Test
  void 해시가_기존과_다르면_결과를_반환한다() {
    ContentEmbeddingTarget target = target("옛날-해시");

    ContentEmbeddingResult result = processor.process(target);

    assertThat(result).isNotNull();
    assertThat(result.contentId()).isEqualTo(target.contentId());
    assertThat(result.title()).isEqualTo(target.title());
    assertThat(result.thumbnailUrl()).isEqualTo(target.thumbnailUrl());
    assertThat(result.text()).isEqualTo(EmbeddingTextBuilder.build(target));
    assertThat(result.sourceHash()).isEqualTo(expectedHash(target));
  }

  @Test
  void 기존_해시가_없으면_변경으로_보고_결과를_반환한다() {
    ContentEmbeddingTarget target = target(null);

    assertThat(processor.process(target)).isNotNull();
  }

  private String expectedHash(ContentEmbeddingTarget target) {
    return EmbeddingTextBuilder.hash(EmbeddingTextBuilder.build(target), MODEL, target.thumbnailUrl());
  }

  private ContentEmbeddingTarget target(String currentSourceHash) {
    return new ContentEmbeddingTarget("c1", "제목", "설명", "https://thumb", "태그", currentSourceHash);
  }
}
