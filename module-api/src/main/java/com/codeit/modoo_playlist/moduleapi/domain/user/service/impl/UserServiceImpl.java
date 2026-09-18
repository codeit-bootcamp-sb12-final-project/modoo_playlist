package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.query.UserQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.UserService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserProfileUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.CursorResponseUserDto;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import com.codeit.modoo_playlist.moduleapi.security.jwt.LoginSessionStore;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final LoginSessionStore loginSessionStore;

  @Transactional
  @Override
  public UserDto create(UserCreateRequest request) {
//    핸들러에서 409로 처리 중
    if (userRepository.existsByEmail(request.email())) {
      throw new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    String encodedPassword = passwordEncoder.encode(request.password());

    User user = User.create(
        request.email(),
        request.name(),
        encodedPassword
    );

    User savedUser = userRepository.save(user);
//    유저 동시 저장시 unique로 DB는 409 conflict
//    서비스에서 이메일 중복시 409 email_already_exists
//    아래 처럼 db의 에러메시지와 서비스의 에러메시지를 통일 가능함.
//    try {
//      User savedUser = userRepository.saveAndFlush(user);
//      return userMapper.toDto(savedUser);
//
//    } catch (DataIntegrityViolationException e) {
//      throw new BaseException(
//          ErrorCode.EMAIL_ALREADY_EXISTS,
//          e
//      );
//    }

    return userMapper.toDto(savedUser);
  }

  @Transactional(readOnly = true)
  @Override
  public UserDto getUser(UUID userId) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    return userMapper.toDto(user);
  }

  @Transactional(readOnly = true)
  @Override
  public CursorResponseUserDto getAllUsers(
      UserListRequest request
  ) {
    UserQueryPage page = userRepository.findAllUsers(request);

    List<UserDto> data = page.users().stream()
        .map(userMapper::toDto)
        .toList();

    return new CursorResponseUserDto(
        data,
        page.nextCursor(),
        page.nextIdAfter(),
        page.hasNext(),
        page.totalCount(),
        request.sortBy(),
        request.sortDirection()
    );
  }

  @Transactional
  @Override
  public UserDto updateUser(
      UUID actorId,
      UUID userId,
      UserProfileUpdateRequest request,
      MultipartFile image
  ) {
    validateOwner(actorId, userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    String imageUrl = null;
    if (image != null && !image.isEmpty()) {
      imageUrl = image.getOriginalFilename();
//      TODO: 이미지 저장 후 URL로 받아오는 메서드로 변경
    }

    user.updateProfile(request.name(), imageUrl);
    return userMapper.toDto(user);
  }

  @Transactional
  @Override
  public void updatePassword(
      UUID actorId,
      UUID userId,
      String newPassword
  ) {
    validateOwner(actorId, userId);

    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    String encodedPassword = passwordEncoder.encode(newPassword);

    user.changePassword(encodedPassword);

    loginSessionStore.invalidateAll(userId);
  }

  @Transactional
  @Override
  public void updateRole(
      UUID actorId,
      UUID userId,
      UserRole role
  ) {
    validateAssignableRole(role);
    validateNotSelf(actorId, userId);

    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    validateManageableUser(user);

    if (user.getRole() == role) {
      return;
    }

    user.changeRole(role);
    loginSessionStore.invalidateAll(userId);
  }

  @Transactional
  @Override
  public void updateLocked(
      UUID actorId,
      UUID userId,
      boolean locked
  ) {
    validateNotSelf(actorId, userId);

    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    validateManageableUser(user);

    if (user.isLocked() == locked) {
      return;
    }

    user.changeLocked(locked);

    if (locked) {
      loginSessionStore.invalidateAll(userId);
    }
  }

  private void validateOwner(UUID actorId, UUID userId) {
    if (!Objects.equals(actorId, userId)) {
      throw new BaseException(ErrorCode.ACCESS_DENIED);
    }
  }

  //  BOT은 권한 변경 막음
  private void validateAssignableRole(UserRole role) {
    if (role != UserRole.ADMIN && role != UserRole.USER) {
      throw new BaseException(ErrorCode.INVALID_REQUEST);
    }
  }

  //  admin이 본인 변경은 막음
  private void validateNotSelf(UUID actorId, UUID userId) {
    if (actorId == null || Objects.equals(actorId, userId)) {
      throw new BaseException(ErrorCode.ACCESS_DENIED);
    }
  }

  //  BOT -> 나머지 역할로의 접근 방지
  private void validateManageableUser(User user) {
    if (user.getRole() == UserRole.BOT) {
      throw new BaseException(ErrorCode.ACCESS_DENIED);
    }
  }
}
