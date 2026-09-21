package com.codeit.modoo_playlist.modulebatch.reviewsummary.writer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryResult;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.persistence.ReviewSummaryMapper;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryWriterTest {

  @Mock private ReviewSummaryMapper mapper;

  @Test
  void 청크의_각_결과를_upsert한다() throws Exception {
    ReviewSummaryResult first = new ReviewSummaryResult("c1", "요약1");
    ReviewSummaryResult second = new ReviewSummaryResult("c2", "요약2");

    new ReviewSummaryWriter(mapper).write(new Chunk<>(List.of(first, second)));

    verify(mapper).upsertSummary("c1", "요약1");
    verify(mapper).upsertSummary("c2", "요약2");
    verifyNoMoreInteractions(mapper);
  }

  @Test
  void 빈_청크는_아무것도_하지_않는다() throws Exception {
    new ReviewSummaryWriter(mapper).write(new Chunk<>(List.of()));

    verifyNoInteractions(mapper);
  }
}
