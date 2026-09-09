package com.codeit.modoo_playlist.moduleapi.domain.user.service;

import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserProfileUpdateRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

  UserDto create(UserCreateRequest request);

  UserDto getUser(UUID userId);

  UserDto updateUser(
      UUID userId,
      UserProfileUpdateRequest request,
      MultipartFile image
  );
}
