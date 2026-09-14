package com.codeit.modoo_playlist.modulebatch.embedding.processor;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class EmbeddingTextBuilder {

  private EmbeddingTextBuilder() {
  }

  public static String build(ContentEmbeddingTarget target) {
    return String.join("\n",
        target.title() == null ? "" : target.title(),
        target.description() == null ? "" : target.description(),
        target.tagNames() == null ? "" : target.tagNames()
    );
  }

  public static String hash(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
