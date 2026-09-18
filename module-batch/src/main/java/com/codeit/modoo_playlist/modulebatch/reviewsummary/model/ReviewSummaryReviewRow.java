package com.codeit.modoo_playlist.modulebatch.reviewsummary.model;

import java.math.BigDecimal;

public record ReviewSummaryReviewRow(
    String contentId,
    String text,
    BigDecimal rating
) {

}
