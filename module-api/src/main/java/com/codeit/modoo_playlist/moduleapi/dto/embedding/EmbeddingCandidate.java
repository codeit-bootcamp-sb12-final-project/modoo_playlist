package com.codeit.modoo_playlist.moduleapi.dto.embedding;

import java.util.List;
import java.util.UUID;

public record EmbeddingCandidate(
    UUID contentId,
    String title,
    String thumbnailUrl,
    List<Double> vector
) {

}
