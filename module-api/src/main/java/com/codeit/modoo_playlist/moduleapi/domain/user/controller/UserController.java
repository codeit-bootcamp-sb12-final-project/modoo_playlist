package com.codeit.modoo_playlist.moduleapi.domain.user.controller;

import com.codeit.modoo_playlist.moduleapi.domain.user.service.UserService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserLockUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserProfileUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserRoleUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.response.CursorResponseUserDto;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  @PostMapping(
      name = "회원가입"
  )
  public ResponseEntity<UserDto> create(
      @Valid @RequestBody UserCreateRequest request
  ) {
    return ResponseEntity.ok(userService.create(request));
  }

  @GetMapping(
      name = "사용자 상세 조회",
      value = "/{userId}"
  )
  public ResponseEntity<UserDto> getUserInfo(
      @PathVariable("userId") UUID userId
  ) {
    return ResponseEntity.ok(userService.getUser(userId));
  }

  @PatchMapping(
      name = "프로필 변경(username, image(not Required)",
      value = "/{userId}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  public ResponseEntity<UserDto> updateUser(
      @PathVariable("userId") UUID userId,
      @Valid @RequestPart("request") UserProfileUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image
  ) {
    return ResponseEntity.ok(userService.updateUser(userId, request, image));
  }

  @PatchMapping(
      name = "비밀번호 변경",
      value = "/{userId}/password"
  )
  public ResponseEntity<Void> updateUserPassword(
      @PathVariable("userId") UUID userId
  ) {
    throw new UnsupportedOperationException("구현 예정");
  }

  @GetMapping(
      name = "[ADMIN 권한] 사용자 목록 조회"
  )
  public ResponseEntity<CursorResponseUserDto> getAllUsers() {
    throw new UnsupportedOperationException("구현 예정");
  }

  @PatchMapping(
      name = "[ADMIN 권한] 권한 수정",
      value = "/{userId}/role"
  )
  public ResponseEntity<Void> updateUserRole(
      @PathVariable UUID userId,
      @RequestBody UserRoleUpdateRequest request
  ) {
    throw new UnsupportedOperationException("구현 예정");
  }

  @PatchMapping(
      name = "[ADMIN 권한] 계정 잠금 상태 변경",
      value = "/{userId}/locked"
  )
  public ResponseEntity<Void> updateUserLock(
      @PathVariable UUID userId,
      @RequestBody UserLockUpdateRequest request
  ) {
    throw new UnsupportedOperationException("구현 예정");
  }
}
