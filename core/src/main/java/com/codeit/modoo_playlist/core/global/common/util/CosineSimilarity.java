package com.codeit.modoo_playlist.core.global.common.util;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CosineSimilarity {
  // IDF = log(전체 콘텐츠 수 / 이 태그가 붙은 콘텐츠 수)
  public static double idf(int contentCount, long totalContentCount) {
    return contentCount <= 0 ? 0.0 : Math.log((double) totalContentCount / contentCount);
  }

  // 코사인 유사도 = (A·B 내적) / (A 크기 * B 크기)
  public static <K> double compute(Map<K, Double> weightsA, Map<K, Double> weightsB) {
    double dot = weightsA.entrySet().stream()
        .mapToDouble(e -> e.getValue() * weightsB.getOrDefault(e.getKey(), 0.0))
        .sum();
    double normA = Math.sqrt(weightsA.values().stream().mapToDouble(w -> w * w).sum());
    double normB = Math.sqrt(weightsB.values().stream().mapToDouble(w -> w * w).sum());
    return (normA == 0 || normB == 0) ? 0.0 : dot / (normA * normB);
  }

  public static double compute(List<Double> vectorA, List<Double> vectorB) {
    if (vectorA.size() != vectorB.size()) {
      throw new IllegalArgumentException(
          "벡터 차원이 다릅니다: " + vectorA.size() + " vs " + vectorB.size());
    }
    double dot = 0, normA = 0, normB = 0;
    for (int i = 0; i < vectorA.size(); i++) {
      double a = vectorA.get(i);
      double b = vectorB.get(i);
      dot += a * b;
      normA += a * a;
      normB += b * b;
    }
    return (normA == 0 || normB == 0) ? 0.0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
  }

}
