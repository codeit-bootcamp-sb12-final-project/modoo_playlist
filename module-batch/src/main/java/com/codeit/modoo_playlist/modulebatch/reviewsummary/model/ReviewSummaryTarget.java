package com.codeit.modoo_playlist.modulebatch.reviewsummary.model;

import java.math.BigDecimal;
import java.util.List;

public record ReviewSummaryTarget(
    String contentId,
    String title,
    List<ReviewItem> reviews
) {

  public record ReviewItem(String text, BigDecimal rating) {

  }
}
