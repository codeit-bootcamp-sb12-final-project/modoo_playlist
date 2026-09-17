package com.codeit.modoo_playlist.modulebatch.reviewsummary.writer;

import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryResult;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.persistence.ReviewSummaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

@Slf4j
@RequiredArgsConstructor
public class ReviewSummaryWriter implements ItemWriter<ReviewSummaryResult> {

  private final ReviewSummaryMapper mapper;

  @Override
  public void write(Chunk<? extends ReviewSummaryResult> chunk) {
    for (ReviewSummaryResult result : chunk) {
      mapper.upsertSummary(result.contentId(), result.summary());
    }
    log.info("리뷰 요약 갱신: {}건", chunk.size());
  }
}
