package com.codeit.modoo_playlist.moduleapi.domain.review.repository;

import com.codeit.modoo_playlist.core.domain.review.entity.Review;
import java.util.UUID;

import com.codeit.modoo_playlist.moduleapi.domain.review.repository.query.ReviewQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewQueryRepository {

    boolean existsByContentIdAndAuthorId(UUID contentId, UUID authorId);

    boolean existsByIdAndAuthorId(UUID id, UUID authorId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Review r where r.authorId = :userId")
    int deleteAllByAuthorId(@Param("userId") UUID userId);

}
