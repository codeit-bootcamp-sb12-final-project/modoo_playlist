package com.codeit.modoo_playlist.modulebatch.embedding.processor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;

class EmbeddingTextBuilderTest {

  @Test
  void 제목_설명_태그를_하나의_문서_포맷으로_합친다() {
    ContentEmbeddingTarget target = target("영화", "우주를 배경으로 한 드라마", "SF 드라마");

    assertThat(EmbeddingTextBuilder.build(target))
        .isEqualTo("title: 영화 | text: 우주를 배경으로 한 드라마 SF 드라마");
  }

  @Test
  void 제목이_없으면_none으로_대체한다() {
    ContentEmbeddingTarget target = target(null, "설명", "태그");

    assertThat(EmbeddingTextBuilder.build(target)).startsWith("title: none | text:");
  }

  @Test
  void 설명과_태그가_모두_없으면_본문은_비어있다() {
    ContentEmbeddingTarget target = target("영화", null, null);

    assertThat(EmbeddingTextBuilder.build(target)).isEqualTo("title: 영화 | text: ");
  }

  @Test
  void 같은_입력이면_해시가_동일하다() {
    String hash1 = EmbeddingTextBuilder.hash("text", "model", "https://thumb");
    String hash2 = EmbeddingTextBuilder.hash("text", "model", "https://thumb");

    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  void 텍스트가_다르면_해시도_다르다() {
    String hash1 = EmbeddingTextBuilder.hash("text-a", "model", "https://thumb");
    String hash2 = EmbeddingTextBuilder.hash("text-b", "model", "https://thumb");

    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  void 썸네일이_null이면_빈문자열과_동일하게_취급한다() {
    String withNull = EmbeddingTextBuilder.hash("text", "model", null);
    String withEmpty = EmbeddingTextBuilder.hash("text", "model", "");

    assertThat(withNull).isEqualTo(withEmpty);
  }

  private ContentEmbeddingTarget target(String title, String description, String tagNames) {
    return new ContentEmbeddingTarget("content-id", title, description, "https://thumb", tagNames, null);
  }
}
