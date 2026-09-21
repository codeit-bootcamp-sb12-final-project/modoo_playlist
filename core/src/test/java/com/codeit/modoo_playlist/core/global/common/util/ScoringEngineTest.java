package com.codeit.modoo_playlist.core.global.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;

class ScoringEngineTest {

  @Test
  void LIKE는_양의_가중치를_갖는다() {
    assertThat(ScoringEngine.weight(InteractionType.LIKE, null, 1)).isCloseTo(3.0, within(1e-9));
  }

  @Test
  void DISLIKE는_음의_가중치를_갖는다() {
    assertThat(ScoringEngine.weight(InteractionType.DISLIKE, null, 1)).isCloseTo(-3.0, within(1e-9));
  }

  @Test
  void NOT_INTERESTED는_음의_가중치를_갖는다() {
    assertThat(ScoringEngine.weight(InteractionType.NOT_INTERESTED, null, 1)).isCloseTo(-2.0, within(1e-9));
  }

  @Test
  void PLAYLIST_ADD와_MARK_WATCHED는_양의_가중치를_갖는다() {
    assertThat(ScoringEngine.weight(InteractionType.PLAYLIST_ADD, null, 1)).isCloseTo(2.0, within(1e-9));
    assertThat(ScoringEngine.weight(InteractionType.MARK_WATCHED, null, 1)).isCloseTo(2.0, within(1e-9));
  }

  @Test
  void VIEW와_TRAILER_WATCH는_취향_신호로_보지_않는다() {
    assertThat(ScoringEngine.weight(InteractionType.VIEW, null, 1)).isZero();
    assertThat(ScoringEngine.weight(InteractionType.TRAILER_WATCH, null, 1)).isZero();
  }

  @Test
  void REVIEW_WRITE는_평점_4점_이상이면_양의_가중치를_갖는다() {
    assertThat(ScoringEngine.weight(InteractionType.REVIEW_WRITE, new BigDecimal("4.0"), 1))
        .isCloseTo(3.0, within(1e-9));
  }

  @Test
  void REVIEW_WRITE는_평점_2점_이하이면_음의_가중치를_갖는다() {
    assertThat(ScoringEngine.weight(InteractionType.REVIEW_WRITE, new BigDecimal("2.0"), 1))
        .isCloseTo(-3.0, within(1e-9));
  }

  @Test
  void REVIEW_WRITE는_평점이_2와_4_사이면_중립이다() {
    assertThat(ScoringEngine.weight(InteractionType.REVIEW_WRITE, new BigDecimal("3.0"), 1)).isZero();
  }

  @Test
  void REVIEW_WRITE는_값이_없으면_중립이다() {
    assertThat(ScoringEngine.weight(InteractionType.REVIEW_WRITE, null, 1)).isZero();
  }

  @Test
  void WATCH_SESSION은_1시간_이상이면_양의_가중치를_갖는다() {
    assertThat(ScoringEngine.weight(InteractionType.WATCH_SESSION, new BigDecimal("3600"), 1))
        .isCloseTo(3.0, within(1e-9));
  }

  @Test
  void WATCH_SESSION은_5분_미만이면_음의_가중치를_갖는다() {
    assertThat(ScoringEngine.weight(InteractionType.WATCH_SESSION, new BigDecimal("299"), 1))
        .isCloseTo(-1.0, within(1e-9));
  }

  @Test
  void WATCH_SESSION은_5분_이상_1시간_미만이면_중립이다() {
    assertThat(ScoringEngine.weight(InteractionType.WATCH_SESSION, new BigDecimal("1000"), 1)).isZero();
  }

  @Test
  void WATCH_SESSION은_값이_없으면_중립이다() {
    assertThat(ScoringEngine.weight(InteractionType.WATCH_SESSION, null, 1)).isZero();
  }

  @Test
  void 발생횟수는_5회를_상한으로_한다() {
    double cappedAtFive = ScoringEngine.weight(InteractionType.LIKE, null, 5);
    double tenOccurrences = ScoringEngine.weight(InteractionType.LIKE, null, 10);

    assertThat(tenOccurrences).isCloseTo(cappedAtFive, within(1e-9));
    assertThat(tenOccurrences).isCloseTo(15.0, within(1e-9));
  }
}
