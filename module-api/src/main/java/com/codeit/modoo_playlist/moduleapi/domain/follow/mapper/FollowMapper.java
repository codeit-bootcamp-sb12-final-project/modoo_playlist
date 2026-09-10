package com.codeit.modoo_playlist.moduleapi.domain.follow.mapper;

import com.codeit.modoo_playlist.core.domain.follow.entity.Follow;
import com.codeit.modoo_playlist.moduleapi.dto.follow.response.FollowResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FollowMapper {

    FollowResponse toResponse(Follow follow);

}