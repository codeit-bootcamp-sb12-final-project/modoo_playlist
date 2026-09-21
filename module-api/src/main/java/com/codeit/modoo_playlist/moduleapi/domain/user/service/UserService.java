package com.codeit.modoo_playlist.moduleapi.domain.user.service;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserProfileUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.CursorResponseUserDto;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.WithdrawalInfoResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

  UserDto create(UserCreateRequest request);

  UserDto getUser(UUID userId);

  WithdrawalInfoResponse getWithdrawalInfo(UUID userId);

  CursorResponseUserDto getAllUsers(UserListRequest request);

  UserDto updateUser(
      UUID actorId,
      UUID userId,
      UserProfileUpdateRequest request,
      MultipartFile image
  );

  void updatePassword(UUID actorId, UUID userId, String newPassword);

  void updateRole(UUID actorId, UUID userId, UserRole role);

  void updateLocked(UUID actorId, UUID userId, boolean locked);
}
