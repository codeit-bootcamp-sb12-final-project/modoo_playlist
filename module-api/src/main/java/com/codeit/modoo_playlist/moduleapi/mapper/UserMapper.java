package com.codeit.modoo_playlist.moduleapi.mapper;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  //  명세대로 name으로 일단 매퍼 수정. 수정 가능성 있음.
  @Mapping(target = "name", source = "username")
  @Mapping(target = "scheduledDeletionAt", ignore = true)
  UserDto toDto(User user);

  @Mapping(target = "userId", source = "id")
  @Mapping(target = "name", source = "username")
  @Mapping(target = "profileImageUrl", source = "profileImageUrl")
  UserSummaryResponse toSummary(User user);
}
