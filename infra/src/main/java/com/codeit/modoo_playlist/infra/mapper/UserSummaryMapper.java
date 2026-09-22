package com.codeit.modoo_playlist.infra.mapper;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.dto.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserSummaryMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "name", source = "username")
    @Mapping(target = "profileImageUrl", source = "profileImageUrl")
    UserSummaryResponse toSummary(User user);
}
