package com.codeit.modoo_playlist.modulebatch.recommendation.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.InteractionTagSignal;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.PreferenceTagRow;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.TagContentCount;
import com.codeit.modoo_playlist.modulebatch.recommendation.persistence.RecommendationRecalcMapper;

@ExtendWith(MockitoExtension.class)
class PreferenceRecalcTaskletTest {

  @Mock private RecommendationRecalcMapper mapper;

  @Test
  void 상호작용_신호가_없으면_upsert없이_종료한다() throws Exception {
    when(mapper.findInteractionTagSignals()).thenReturn(List.of());

    RepeatStatus status = new PreferenceRecalcTasklet(mapper).execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(mapper, never()).upsertPreferenceTags(anyList());
  }

  @Test
  void 같은_사용자_태그_쌍의_신호를_합산하고_최신_신호시각을_기록한다() throws Exception {
    Instant older = Instant.parse("2026-01-01T00:00:00Z");
    Instant newer = Instant.parse("2026-02-01T00:00:00Z");
    when(mapper.findInteractionTagSignals()).thenReturn(List.of(
        new InteractionTagSignal("u1", "t1", InteractionType.LIKE, null, 1, older),
        new InteractionTagSignal("u1", "t1", InteractionType.LIKE, null, 1, newer)
    ));
    when(mapper.findTagContentCounts()).thenReturn(List.of(new TagContentCount("t1", 10)));
    when(mapper.countActiveContents()).thenReturn(100L);

    new PreferenceRecalcTasklet(mapper).execute(null, null);

    ArgumentCaptor<List<PreferenceTagRow>> captor = captor();
    verify(mapper).upsertPreferenceTags(captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    PreferenceTagRow row = captor.getValue().get(0);
    assertThat(row.userId()).isEqualTo("u1");
    assertThat(row.tagId()).isEqualTo("t1");
    assertThat(row.rawScore()).isCloseTo(6.0, within(1e-9));
    assertThat(row.lastSignalAt()).isEqualTo(newer);
  }

  @Test
  void 서로_다른_사용자_태그_쌍은_별도_행으로_계산한다() throws Exception {
    Instant now = Instant.parse("2026-01-01T00:00:00Z");
    when(mapper.findInteractionTagSignals()).thenReturn(List.of(
        new InteractionTagSignal("u1", "t1", InteractionType.LIKE, null, 1, now),
        new InteractionTagSignal("u2", "t2", InteractionType.DISLIKE, null, 1, now)
    ));
    when(mapper.findTagContentCounts()).thenReturn(List.of());
    when(mapper.countActiveContents()).thenReturn(10L);

    new PreferenceRecalcTasklet(mapper).execute(null, null);

    ArgumentCaptor<List<PreferenceTagRow>> captor = captor();
    verify(mapper).upsertPreferenceTags(captor.capture());
    assertThat(captor.getValue()).extracting(PreferenceTagRow::userId, PreferenceTagRow::tagId)
        .containsExactlyInAnyOrder(tuple("u1", "t1"), tuple("u2", "t2"));
  }

  @Test
  void 태그의_콘텐츠_수가_집계에_없으면_0으로_취급해도_예외가_나지_않는다() throws Exception {
    when(mapper.findInteractionTagSignals()).thenReturn(List.of(
        new InteractionTagSignal("u1", "t-unknown", InteractionType.LIKE, null, 1, Instant.now())
    ));
    when(mapper.findTagContentCounts()).thenReturn(List.of());
    when(mapper.countActiveContents()).thenReturn(10L);

    assertThatCode(() -> new PreferenceRecalcTasklet(mapper).execute(null, null))
        .doesNotThrowAnyException();
  }

  @Test
  void 건수가_많으면_천건_단위로_나눠서_upsert한다() throws Exception {
    List<InteractionTagSignal> signals = new ArrayList<>();
    for (int i = 0; i < 1500; i++) {
      signals.add(new InteractionTagSignal("u" + i, "t" + i, InteractionType.LIKE, null, 1, Instant.now()));
    }
    when(mapper.findInteractionTagSignals()).thenReturn(signals);
    when(mapper.findTagContentCounts()).thenReturn(List.of());
    when(mapper.countActiveContents()).thenReturn(10L);

    new PreferenceRecalcTasklet(mapper).execute(null, null);

    ArgumentCaptor<List<PreferenceTagRow>> captor = captor();
    verify(mapper, times(2)).upsertPreferenceTags(captor.capture());
    List<List<PreferenceTagRow>> chunks = captor.getAllValues();
    assertThat(chunks.get(0)).hasSize(1000);
    assertThat(chunks.get(1)).hasSize(500);
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<List<PreferenceTagRow>> captor() {
    return ArgumentCaptor.forClass(List.class);
  }
}
