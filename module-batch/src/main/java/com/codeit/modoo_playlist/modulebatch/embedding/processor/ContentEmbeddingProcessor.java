package com.codeit.modoo_playlist.modulebatch.embedding.processor;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingResult;
import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemProcessor;

@RequiredArgsConstructor
public class ContentEmbeddingProcessor implements
    ItemProcessor<ContentEmbeddingTarget, ContentEmbeddingResult> {

  private final String configuredModel;

  @Override
  public @Nullable ContentEmbeddingResult process(ContentEmbeddingTarget target) {
    String text = EmbeddingTextBuilder.build(target);
    String newHash = EmbeddingTextBuilder.hash(text, configuredModel, target.thumbnailUrl());

    if (newHash.equals(target.currentSourceHash())) {
      return null;
    }

    return new ContentEmbeddingResult(
        target.contentId(), text, target.title(), target.thumbnailUrl(), newHash);
  }
}
