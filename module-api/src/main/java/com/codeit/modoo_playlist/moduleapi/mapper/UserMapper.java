package com.codeit.modoo_playlist.moduleapi.mapper;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  //  명세대로 name으로 일단 매퍼 수정. 수정 가능성 있음.
  @Mapping(target = "name", source = "username")
  UserDto toDto(User user);
}
