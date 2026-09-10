package com.codeit.modoo_playlist.moduleapi.domain.review.mapper;

import com.codeit.modoo_playlist.core.domain.review.entity.Review;
import com.codeit.modoo_playlist.moduleapi.dto.review.response.ReviewResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ReviewResponse toResponse(Review review);
}