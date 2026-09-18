package com.codeit.modoo_playlist.modulebatch.reviewsummary.reader;

import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryCandidate;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryReviewRow;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.model.ReviewSummaryTarget.ReviewItem;
import com.codeit.modoo_playlist.modulebatch.reviewsummary.persistence.ReviewSummaryMapper;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemReader;

public class ReviewSummaryReader implements ItemReader<ReviewSummaryTarget> {

  private static final int PAGE_SIZE = 100;

  private final ReviewSummaryMapper mapper;
  private final int minReviews;
  private final int maxReviewsPerContent;
  private final int maxItems;

  private int emitted = 0;
  private @Nullable String lastContentId = null;
  private Iterator<ReviewSummaryTarget> currentPage = List.<ReviewSummaryTarget>of().iterator();

  public ReviewSummaryReader(
      ReviewSummaryMapper mapper, int minReviews, int maxReviewsPerContent, int maxItems) {
    this.mapper = mapper;
    this.minReviews = minReviews;
    this.maxReviewsPerContent = maxReviewsPerContent;
    this.maxItems = maxItems;
  }

  @Override
  public @Nullable ReviewSummaryTarget read() {
    if (emitted >= maxItems) {
      return null;
    }
    if (!currentPage.hasNext()) {
      List<ReviewSummaryTarget> page = fetchPage();
      if (page.isEmpty()) {
        return null;
      }
      currentPage = page.iterator();
    }
    emitted++;
    return currentPage.next();
  }

  private List<ReviewSummaryTarget> fetchPage() {
    List<ReviewSummaryCandidate> candidates =
        mapper.findContentsNeedingSummary(minReviews, lastContentId, PAGE_SIZE);
    if (candidates.isEmpty()) {
      return List.of();
    }
    lastContentId = candidates.get(candidates.size() - 1).contentId();

    List<String> contentIds = candidates.stream().map(ReviewSummaryCandidate::contentId).toList();
    List<ReviewSummaryReviewRow> rows = mapper.findReviewsForContents(contentIds, maxReviewsPerContent);
    Map<String, List<ReviewItem>> reviewsByContent = rows.stream()
        .collect(Collectors.groupingBy(
            ReviewSummaryReviewRow::contentId,
            LinkedHashMap::new,
            Collectors.mapping(row -> new ReviewItem(row.text(), row.rating()), Collectors.toList())));

    return candidates.stream()
        .map(candidate -> new ReviewSummaryTarget(
            candidate.contentId(),
            candidate.title(),
            reviewsByContent.getOrDefault(candidate.contentId(), List.of())))
        .toList();
  }
}
