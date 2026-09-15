package com.codeit.modoo_playlist.modulebatch.embedding.processor;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class EmbeddingTextBuilder {

  private static final String STORE_GENERATION = "es-v1";

  private EmbeddingTextBuilder() {
  }

  // Gemini 비대칭 검색 문서 포맷. 쿼리 쪽은 module-api의 SearchQueryTextBuilder가 짝을 맞춤
  public static String build(ContentEmbeddingTarget target) {
    String title = target.title() == null ? "none" : target.title();
    String body = String.join(" ",
        target.description() == null ? "" : target.description(),
        target.tagNames() == null ? "" : target.tagNames()
    ).trim();
    return "title: " + title + " | text: " + body;
  }

  public static String hash(String text, String model, String thumbnailUrl) {
    String normalizedThumbnail = thumbnailUrl == null ? "" : thumbnailUrl;
    return sha256(model + "::" + STORE_GENERATION + "::" + normalizedThumbnail + "::" + text);
  }

  private static String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
