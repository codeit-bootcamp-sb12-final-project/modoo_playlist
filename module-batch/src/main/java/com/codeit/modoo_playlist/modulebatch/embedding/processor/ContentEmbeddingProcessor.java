package com.codeit.modoo_playlist.modulebatch.embedding.processor;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingResult;
import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.batch.infrastructure.item.ItemProcessor;

@RequiredArgsConstructor
public class ContentEmbeddingProcessor implements
    ItemProcessor<ContentEmbeddingTarget, ContentEmbeddingResult> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final EmbeddingModel embeddingModel;

  @Override
  public @Nullable ContentEmbeddingResult process(ContentEmbeddingTarget target) {
    String text = EmbeddingTextBuilder.build(target);
    String newHash = EmbeddingTextBuilder.hash(text);

    if (newHash.equals(target.currentSourceHash())) {
      return null;
    }

    EmbeddingResponse response = embeddingModel.call(
        new EmbeddingRequest(List.of(text), EmbeddingOptions.builder().build()));

    float[] vector = response.getResult().getOutput();
    List<Double> vectorList = new ArrayList<>(vector.length);
    for (float v : vector) {
      vectorList.add((double) v);
    }
    String model = response.getMetadata().getModel();

    String vectorJson;
    try {
      vectorJson = OBJECT_MAPPER.writeValueAsString(vectorList);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("벡터 직렬화 실패: " + target.contentId(), e);
    }

    return new ContentEmbeddingResult(target.contentId(), vectorJson, vector.length, model, newHash);
  }
}
