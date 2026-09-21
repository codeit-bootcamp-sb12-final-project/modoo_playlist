package com.codeit.modoo_playlist.modulebatch.recommendation.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TagScoreCalculatorTest {

  @Test
  void decay는_lastSignalAt이_없으면_0이다() {
    assertThat(TagScoreCalculator.decay(null, Instant.now())).isZero();
  }

  @Test
  void decay는_신호가_현재나_미래_시각이면_감쇠없이_1이다() {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");

    assertThat(TagScoreCalculator.decay(now, now)).isCloseTo(1.0, within(1e-9));
    assertThat(TagScoreCalculator.decay(now.plusSeconds(3600), now)).isCloseTo(1.0, within(1e-9));
  }

  @Test
  void decay는_반감기가_지나면_절반이_된다() {
    Instant lastSignalAt = Instant.parse("2026-01-01T00:00:00Z");
    Instant now = lastSignalAt.plusSeconds(30L * 86400);

    assertThat(TagScoreCalculator.decay(lastSignalAt, now)).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void decay는_반감기의_두배가_지나면_4분의1이_된다() {
    Instant lastSignalAt = Instant.parse("2026-01-01T00:00:00Z");
    Instant now = lastSignalAt.plusSeconds(60L * 86400);

    assertThat(TagScoreCalculator.decay(lastSignalAt, now)).isCloseTo(0.25, within(1e-9));
  }

  @Test
  void score는_원점수와_시간감쇠와_IDF의_곱이다() {
    Instant lastSignalAt = Instant.parse("2026-01-01T00:00:00Z");
    Instant now = lastSignalAt.plusSeconds(30L * 86400);

    double score = TagScoreCalculator.score(10.0, lastSignalAt, now, 10, 100);

    assertThat(score).isCloseTo(10.0 * 0.5 * Math.log(10.0), within(1e-9));
  }

  @Test
  void score는_신호가_없으면_0이다() {
    assertThat(TagScoreCalculator.score(10.0, null, Instant.now(), 10, 100)).isZero();
  }
}
