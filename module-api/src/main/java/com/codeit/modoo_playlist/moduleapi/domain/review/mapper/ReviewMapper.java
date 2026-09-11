package com.codeit.modoo_playlist.moduleapi.domain.review.mapper;

import com.codeit.modoo_playlist.core.domain.review.entity.Review;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.query.ReviewQueryPage;
import com.codeit.modoo_playlist.moduleapi.dto.review.response.ReviewCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.review.response.ReviewResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ReviewResponse toResponse(Review review, UserSummaryResponse author);

    @Mapping(target = "nextCursor", source = "page.nextCursor")
    @Mapping(target = "nextIdAfter", source = "page.nextIdAfter")
    @Mapping(target = "hasNext", source = "page.hasNext")
    @Mapping(target = "totalCount", source = "page.totalCount")
    ReviewCursorResponse toCursorResponse(
            ReviewQueryPage page,
            List<ReviewResponse> data,
            String sortBy,
            String sortDirection
    );
}