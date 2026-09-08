package com.codeit.modoo_playlist.moduleapi.mapper;

import com.codeit.modoo_playlist.moduleapi.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  public UserDto toDto(User user);
}
