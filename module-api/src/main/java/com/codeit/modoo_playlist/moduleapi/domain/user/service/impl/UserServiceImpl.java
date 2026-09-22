package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.message.repository.MessageRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.ReviewRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.SocialAccountRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageCategory;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageStorage;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.query.UserQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.UserService;
import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserProfileUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.CursorResponseUserDto;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.WithdrawalInfoResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.WithdrawalVerificationMethod;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import java.io.IOException;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import com.codeit.modoo_playlist.moduleapi.security.jwt.LoginSessionStore;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final SocialAccountRepository socialAccountRepository;
  private final MessageRepository messageRepository;
  private final WatchingSessionRepository watchingSessionRepository;
  private final ReviewRepository reviewRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final LoginSessionStore loginSessionStore;
  private final ImageStorage imageStorage;

  @Value("${user-deletion.retention:1d}")
  private Duration userDeletionRetention = Duration.ofDays(1);

  @Value("${user-deletion.zone:Asia/Seoul}")
  private ZoneId userDeletionZone = ZoneId.of("Asia/Seoul");

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
  public WithdrawalInfoResponse getWithdrawalInfo(UUID userId) {
    return socialAccountRepository.findByUserId(userId)
        .map(socialAccount -> new WithdrawalInfoResponse(
            WithdrawalVerificationMethod.OAUTH,
            socialAccount.getProvider()
        ))
        .orElseGet(() -> new WithdrawalInfoResponse(
            WithdrawalVerificationMethod.PASSWORD,
            null
        ));
  }

  @Transactional
  @Override
  public void withdraw(UUID userId, String password) {
    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    if (user.getDeletedAt() != null) {
      throw new BaseException(ErrorCode.USER_ACCOUNT_WITHDRAWN);
    }

    if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
      throw new BaseException(ErrorCode.INVALID_CURRENT_PASSWORD);
    }

    user.withdraw(Instant.now());
    loginSessionStore.invalidateAll(userId);
  }

  @Transactional(readOnly = true)
  @Override
  public CursorResponseUserDto getAllUsers(
      UserListRequest request
  ) {
    UserQueryPage page = userRepository.findAllUsers(request);

    List<UserDto> data = page.users().stream()
        .map(userMapper::toDto)
        .map(this::withScheduledDeletionAt)
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
  public void purgeUser(UUID actorId, UUID userId) {
    validateNotSelf(actorId, userId);

    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    validateManageableUser(user);

    if (user.getDeletedAt() == null) {
      throw new BaseException(ErrorCode.USER_NOT_WITHDRAWN);
    }

    messageRepository.deleteAllByUserId(userId);
    watchingSessionRepository.deleteAllByWatcherId(userId);
    reviewRepository.deleteAllByAuthorId(userId);
    userRepository.delete(user);
    userRepository.flush();
    loginSessionStore.invalidateAll(userId);
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
      try {
        imageUrl = imageStorage.store(image, ImageCategory.USER_PROFILE);
        registerImageRollbackCleanup(imageUrl);
        registerPreviousImageCleanup(user.getProfileImageUrl(), imageUrl);
      } catch (IOException exception) {
        throw new BaseException(ErrorCode.FILE_SAVE_FAILED, exception);
      }
    }

    user.updateProfile(request.name(), imageUrl);
    return userMapper.toDto(user);
  }

  private void registerImageRollbackCleanup(String imageUrl) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status != STATUS_ROLLED_BACK) return;
        deleteImageQuietly(imageUrl);
      }
    });
  }

  private void registerPreviousImageCleanup(String previousImageUrl, String newImageUrl) {
    if (previousImageUrl == null || Objects.equals(previousImageUrl, newImageUrl)
        || !TransactionSynchronizationManager.isSynchronizationActive()) return;
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status != STATUS_COMMITTED) return;
        deleteImageQuietly(previousImageUrl);
      }
    });
  }

  private void deleteImageQuietly(String imageUrl) {
    try {
      imageStorage.delete(imageUrl);
    } catch (IOException ignored) {
      // 이미지 정리 실패로 원래 트랜잭션 결과를 바꾸지 않는다.
    }
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

  private UserDto withScheduledDeletionAt(UserDto user) {
    Instant scheduledAt = calculateScheduledDeletionAt(user.deletedAt());
    return new UserDto(
        user.id(),
        user.email(),
        user.name(),
        user.profileImageUrl(),
        user.role(),
        user.locked(),
        user.createdAt(),
        user.deletedAt(),
        scheduledAt
    );
  }

  private Instant calculateScheduledDeletionAt(Instant deletedAt) {
    if (deletedAt == null) {
      return null;
    }

    ZonedDateTime eligibleAt = deletedAt.plus(userDeletionRetention).atZone(userDeletionZone);
    if (eligibleAt.toLocalTime().equals(LocalTime.MIDNIGHT)) {
      return eligibleAt.toInstant();
    }

    return eligibleAt.toLocalDate()
        .plusDays(1)
        .atStartOfDay(userDeletionZone)
        .toInstant();
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
