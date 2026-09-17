package com.codeit.modoo_playlist.moduleapi.domain.review.repository;

import com.codeit.modoo_playlist.core.domain.review.entity.ContentReviewSummary;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSummaryRepository extends JpaRepository<ContentReviewSummary, UUID> {

}
