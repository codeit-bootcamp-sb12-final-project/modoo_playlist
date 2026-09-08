package com.codeit.modoo_playlist.moduleapi.mapper;

import com.codeit.modoo_playlist.core.global.common.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserDto toDto(User user);
}
