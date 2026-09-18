package com.codeit.modoo_playlist.modulebatch.recommendation.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import com.codeit.modoo_playlist.modulebatch.recommendation.model.SimilarityRow;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.TagName;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.UserTagScore;
import com.codeit.modoo_playlist.modulebatch.recommendation.persistence.RecommendationRecalcMapper;

@ExtendWith(MockitoExtension.class)
class SimilarUserTaskletTest {

  @Mock private RecommendationRecalcMapper mapper;

  @Test
  void 시작하자마자_기존_유사도를_전부_삭제한다() throws Exception {
    when(mapper.findAllPreferenceScores()).thenReturn(List.of());

    new SimilarUserTasklet(mapper).execute(null, null);

    verify(mapper).deleteAllSimilarities();
  }

  @Test
  void 비교대상_사용자가_2명_미만이면_upsert없이_종료한다() throws Exception {
    when(mapper.findAllPreferenceScores()).thenReturn(List.of(new UserTagScore("u1", "t1", 1.0)));

    RepeatStatus status = new SimilarUserTasklet(mapper).execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    verify(mapper, never()).upsertSimilarities(anyList());
  }

  @Test
  void 취향이_같은_두_사용자는_서로를_양방향으로_저장하고_공유태그를_점수순으로_담는다() throws Exception {
    when(mapper.findAllPreferenceScores()).thenReturn(List.of(
        new UserTagScore("u1", "A", 2.0), new UserTagScore("u1", "B", 1.0),
        new UserTagScore("u2", "A", 2.0), new UserTagScore("u2", "B", 1.0)
    ));
    when(mapper.findTagNames()).thenReturn(List.of(new TagName("A", "액션"), new TagName("B", "드라마")));

    new SimilarUserTasklet(mapper).execute(null, null);

    List<SimilarityRow> rows = captureRows();
    assertThat(rows).extracting(SimilarityRow::userId, SimilarityRow::otherUserId)
        .containsExactlyInAnyOrder(tuple("u1", "u2"), tuple("u2", "u1"));
    assertThat(rows).allSatisfy(row -> {
      assertThat(row.score()).isCloseTo(1.0, within(1e-9));
      assertThat(row.sharedTags()).isEqualTo("액션, 드라마");
    });
  }

  @Test
  void 유사도가_0_이하인_사용자는_이웃에서_제외한다() throws Exception {
    when(mapper.findAllPreferenceScores()).thenReturn(List.of(
        new UserTagScore("u1", "A", 1.0),
        new UserTagScore("u2", "A", 1.0),
        new UserTagScore("u3", "C", 1.0)
    ));
    when(mapper.findTagNames()).thenReturn(List.of());

    new SimilarUserTasklet(mapper).execute(null, null);

    List<SimilarityRow> rows = captureRows();
    assertThat(rows).extracting(SimilarityRow::userId).doesNotContain("u3");
    assertThat(rows).extracting(SimilarityRow::otherUserId).doesNotContain("u3");
  }

  @Test
  void 상위_20명까지만_이웃으로_남기고_동점이면_ID순으로_자른다() throws Exception {
    List<UserTagScore> scores = new ArrayList<>();
    scores.add(new UserTagScore("target", "A", 1.0));
    for (int i = 1; i <= 25; i++) {
      scores.add(new UserTagScore("other%02d".formatted(i), "A", 1.0));
    }
    when(mapper.findAllPreferenceScores()).thenReturn(scores);
    when(mapper.findTagNames()).thenReturn(List.of());

    new SimilarUserTasklet(mapper).execute(null, null);

    List<SimilarityRow> rows = captureRows();
    List<String> targetsNeighbors = rows.stream()
        .filter(row -> row.userId().equals("target"))
        .map(SimilarityRow::otherUserId)
        .toList();

    assertThat(targetsNeighbors).hasSize(20);
    assertThat(targetsNeighbors).contains("other01", "other20");
    assertThat(targetsNeighbors).doesNotContain("other21", "other25");
  }

  @SuppressWarnings("unchecked")
  private List<SimilarityRow> captureRows() {
    ArgumentCaptor<List<SimilarityRow>> captor = ArgumentCaptor.forClass(List.class);
    verify(mapper).upsertSimilarities(captor.capture());
    return captor.getValue();
  }
}
