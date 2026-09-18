package com.codeit.modoo_playlist.core.global.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;

import org.junit.jupiter.api.Test;

class CosineSimilarityTest {

  @Test
  void idf는_해당_태그가_붙은_콘텐츠가_많을수록_작아진다() {
    double rare = CosineSimilarity.idf(1, 100);
    double common = CosineSimilarity.idf(50, 100);

    assertThat(rare).isGreaterThan(common);
  }

  @Test
  void idf는_contentCount가_0_이하면_0을_반환한다() {
    assertThat(CosineSimilarity.idf(0, 100)).isZero();
    assertThat(CosineSimilarity.idf(-1, 100)).isZero();
  }

  @Test
  void compute는_동일한_벡터의_유사도를_1로_계산한다() {
    Map<String, Double> vector = Map.of("액션", 2.0, "드라마", 1.0);

    assertThat(CosineSimilarity.compute(vector, vector)).isCloseTo(1.0, within(1e-9));
  }

  @Test
  void compute는_겹치는_차원이_없으면_0이다() {
    Map<String, Double> a = Map.of("액션", 1.0);
    Map<String, Double> b = Map.of("드라마", 1.0);

    assertThat(CosineSimilarity.compute(a, b)).isZero();
  }

  @Test
  void compute는_한쪽이_빈_벡터면_0이다() {
    Map<String, Double> a = Map.of("액션", 1.0);

    assertThat(CosineSimilarity.compute(a, Map.of())).isZero();
    assertThat(CosineSimilarity.compute(Map.of(), a)).isZero();
  }

  @Test
  void compute는_직교하는_벡터를_0으로_계산한다() {
    Map<String, Double> a = Map.of("x", 1.0, "y", 0.0);
    Map<String, Double> b = Map.of("x", 0.0, "y", 1.0);

    assertThat(CosineSimilarity.compute(a, b)).isCloseTo(0.0, within(1e-9));
  }
}
