package com.codeit.modoo_playlist.modulebatch.reviewsummary.persistence;

import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryCandidate;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryReviewRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jspecify.annotations.Nullable;

@Mapper
public interface ReviewSummaryMapper {

  List<ReviewSummaryCandidate> findContentsNeedingSummary(
      @Param("minReviews") int minReviews,
      @Param("lastContentId") @Nullable String lastContentId,
      @Param("limit") int limit
  );

  List<ReviewSummaryReviewRow> findReviewsForContents(
      @Param("contentIds") List<String> contentIds,
      @Param("maxReviewsPerContent") int maxReviewsPerContent
  );

  void upsertSummary(@Param("contentId") String contentId, @Param("summary") String summary);
}
