package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.impl;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CosineSimilarity {
  public static double idf(int contentCount, long totalContentCount) {
    return contentCount <= 0 ? 0.0 : Math.log((double) totalContentCount / contentCount);
  }

  public static double compute(Map<UUID, Double> weightsA, Map<UUID, Double> weightsB) {
    double dot = weightsA.entrySet().stream()
        .mapToDouble(e -> e.getValue() * weightsB.getOrDefault(e.getKey(), 0.0))
        .sum();
    double normA = Math.sqrt(weightsA.values().stream().mapToDouble(w -> w * w).sum());
    double normB = Math.sqrt(weightsB.values().stream().mapToDouble(w -> w * w).sum());
    return (normA == 0 || normB == 0) ? 0.0 : dot / (normA * normB);
  }
}
