package com.codeit.modoo_playlist.moduleapi.domain.review.repository;

import com.codeit.modoo_playlist.core.domain.review.entity.Review;
import java.util.UUID;

import com.codeit.modoo_playlist.moduleapi.domain.review.repository.query.ReviewQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewQueryRepository {

    boolean existsByContentIdAndAuthorId(UUID contentId, UUID authorId);

    boolean existsByIdAndAuthorId(UUID id, UUID authorId);

}