package com.codeit.modoo_playlist.modulebatch.reviewsummary.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryCandidate;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryReviewRow;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.persistence.ReviewSummaryMapper;

@ExtendWith(MockitoExtension.class)
class ReviewSummaryReaderTest {

  @Mock private ReviewSummaryMapper mapper;

  @Test
  void 첫_페이지가_비어있으면_null을_반환한다() throws Exception {
    when(mapper.findContentsNeedingSummary(3, null, 100)).thenReturn(List.of());

    ReviewSummaryReader reader = new ReviewSummaryReader(mapper, 3, 30, 100);

    assertThat(reader.read()).isNull();
  }

  @Test
  void 후보와_리뷰를_콘텐츠별로_그룹핑해서_순서대로_반환한다() throws Exception {
    when(mapper.findContentsNeedingSummary(3, null, 100)).thenReturn(List.of(
        new ReviewSummaryCandidate("c1", "제목1"),
        new ReviewSummaryCandidate("c2", "제목2")
    ));
    when(mapper.findReviewsForContents(List.of("c1", "c2"), 30)).thenReturn(List.of(
        new ReviewSummaryReviewRow("c1", "리뷰1", new BigDecimal("4.0")),
        new ReviewSummaryReviewRow("c1", "리뷰2", new BigDecimal("3.0")),
        new ReviewSummaryReviewRow("c2", "리뷰3", new BigDecimal("5.0"))
    ));
    when(mapper.findContentsNeedingSummary(3, "c2", 100)).thenReturn(List.of());

    ReviewSummaryReader reader = new ReviewSummaryReader(mapper, 3, 30, 100);

    ReviewSummaryTarget first = reader.read();
    assertThat(first.contentId()).isEqualTo("c1");
    assertThat(first.reviews()).extracting(ReviewSummaryTarget.ReviewItem::text)
        .containsExactly("리뷰1", "리뷰2");

    ReviewSummaryTarget second = reader.read();
    assertThat(second.contentId()).isEqualTo("c2");
    assertThat(second.reviews()).extracting(ReviewSummaryTarget.ReviewItem::text)
        .containsExactly("리뷰3");

    assertThat(reader.read()).isNull();
  }

  @Test
  void 매칭되는_리뷰가_없는_후보는_빈_리뷰_목록을_가진다() throws Exception {
    when(mapper.findContentsNeedingSummary(3, null, 100)).thenReturn(
        List.of(new ReviewSummaryCandidate("c1", "제목1")));
    when(mapper.findReviewsForContents(List.of("c1"), 30)).thenReturn(List.of());

    ReviewSummaryReader reader = new ReviewSummaryReader(mapper, 3, 30, 100);

    assertThat(reader.read().reviews()).isEmpty();
  }

  @Test
  void 페이지_전환시_직전_페이지의_마지막_콘텐츠ID를_커서로_넘긴다() throws Exception {
    when(mapper.findContentsNeedingSummary(3, null, 100)).thenReturn(
        List.of(new ReviewSummaryCandidate("c1", "제목1")));
    when(mapper.findReviewsForContents(List.of("c1"), 30)).thenReturn(List.of());
    when(mapper.findContentsNeedingSummary(3, "c1", 100)).thenReturn(List.of());

    ReviewSummaryReader reader = new ReviewSummaryReader(mapper, 3, 30, 100);
    reader.read();
    reader.read();

    verify(mapper).findContentsNeedingSummary(3, "c1", 100);
  }

  @Test
  void maxItems에_도달하면_더_읽지_않고_남은_페이지도_소비하지_않는다() throws Exception {
    when(mapper.findContentsNeedingSummary(3, null, 100)).thenReturn(List.of(
        new ReviewSummaryCandidate("c1", "제목1"),
        new ReviewSummaryCandidate("c2", "제목2")
    ));
    when(mapper.findReviewsForContents(List.of("c1", "c2"), 30)).thenReturn(List.of());

    ReviewSummaryReader reader = new ReviewSummaryReader(mapper, 3, 30, 1);

    assertThat(reader.read()).isNotNull();
    assertThat(reader.read()).isNull();
    verify(mapper, never()).findContentsNeedingSummary(3, "c2", 100);
  }
}
