package com.codeit.modoo_playlist.moduleapi.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.domain.user.entity.SocialAccount;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.message.repository.MessageRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.ReviewRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.SocialAccountRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.query.UserQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageCategory;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageStorage;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.UserServiceImpl;
import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository.ApiWatchingSessionRepository;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserProfileUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.CursorResponseUserDto;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.WithdrawalInfoResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.WithdrawalVerificationMethod;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final String EMAIL = "user@example.com";
  private static final String USERNAME = "tester";
  private static final String PASSWORD = "Password123!";
  private static final String STORED_PASSWORD = "stored-password-hash";
  private static final String ORIGINAL_USERNAME = "original";
  private static final String CHANGED_USERNAME = "changed";
  private static final String ORIGINAL_IMAGE = "https://example.com/original.png";

  @Mock
  UserRepository userRepository;

  @Mock
  SocialAccountRepository socialAccountRepository;

  @Mock
  MessageRepository messageRepository;

  @Mock
  ApiWatchingSessionRepository watchingSessionRepository;

  @Mock
  ReviewRepository reviewRepository;

  @Mock
  LoginSessionStore loginSessionStore;

  @Mock
  ImageStorage imageStorage;

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private UUID userId;
  private UserServiceImpl service;
  private User existingUser;
  private UserCreateRequest createRequest;
  private UserProfileUpdateRequest updateRequest;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    existingUser = User.create(EMAIL, ORIGINAL_USERNAME, STORED_PASSWORD);
    ReflectionTestUtils.setField(existingUser, "id", userId);
    existingUser.updateProfile(ORIGINAL_USERNAME, ORIGINAL_IMAGE);
    createRequest = new UserCreateRequest(EMAIL, USERNAME, PASSWORD);
    updateRequest = new UserProfileUpdateRequest(CHANGED_USERNAME);
    // 저장소만 대체하고 암호화 및 DTO 매핑은 실제 구현을 사용한다.
    service = new UserServiceImpl(
        userRepository,
        socialAccountRepository,
        messageRepository,
        watchingSessionRepository,
        reviewRepository,
        encoder,
        Mappers.getMapper(UserMapper.class),
        loginSessionStore,
        imageStorage
    );
  }

  @Test
  @DisplayName("일반 계정의 탈퇴 인증 방식은 비밀번호이다")
  void getPasswordWithdrawalInfo() {
    when(socialAccountRepository.findByUserId(userId)).thenReturn(Optional.empty());

    WithdrawalInfoResponse result = service.getWithdrawalInfo(userId);

    assertThat(result.verificationMethod()).isEqualTo(WithdrawalVerificationMethod.PASSWORD);
    assertThat(result.provider()).isNull();
  }

  @Test
  @DisplayName("소셜 계정의 탈퇴 인증 방식과 공급자를 반환한다")
  void getOAuthWithdrawalInfo() {
    SocialAccount socialAccount = SocialAccount.create(existingUser, Provider.GOOGLE, "google-sub");
    when(socialAccountRepository.findByUserId(userId)).thenReturn(Optional.of(socialAccount));

    WithdrawalInfoResponse result = service.getWithdrawalInfo(userId);

    assertThat(result.verificationMethod()).isEqualTo(WithdrawalVerificationMethod.OAUTH);
    assertThat(result.provider()).isEqualTo(Provider.GOOGLE);
  }

  @Test
  @DisplayName("관리자 사용자 목록에 탈퇴 시각과 예약 삭제 시각을 포함한다")
  void getAllUsersIncludesScheduledDeletionAt() {
    Instant withdrawnAt = Instant.parse("2026-09-21T07:00:00Z");
    existingUser.withdraw(withdrawnAt);
    UserListRequest request = new UserListRequest(
        null,
        null,
        null,
        null,
        null,
        20,
        "ASCENDING",
        "email"
    );
    when(userRepository.findAllUsers(request)).thenReturn(
        new UserQueryPage(java.util.List.of(existingUser), null, null, false, 1)
    );

    CursorResponseUserDto result = service.getAllUsers(request);

    assertThat(result.data()).singleElement().satisfies(user -> {
      assertThat(user.deletedAt()).isEqualTo(withdrawnAt);
      assertThat(user.scheduledDeletionAt())
          .isEqualTo(Instant.parse("2026-09-22T15:00:00Z"));
    });
  }

  @Test
  @DisplayName("정책 시간이 정확히 자정이면 다음 날로 넘기지 않는다")
  void scheduledDeletionKeepsExactMidnight() {
    Instant withdrawnAt = Instant.parse("2026-09-21T15:00:00Z");
    existingUser.withdraw(withdrawnAt);
    UserListRequest request = new UserListRequest(
        null, null, null, null, null, 20, "ASCENDING", "email"
    );
    when(userRepository.findAllUsers(request)).thenReturn(
        new UserQueryPage(java.util.List.of(existingUser), null, null, false, 1)
    );

    CursorResponseUserDto result = service.getAllUsers(request);

    assertThat(result.data()).singleElement().satisfies(user ->
        assertThat(user.scheduledDeletionAt())
            .isEqualTo(Instant.parse("2026-09-22T15:00:00Z"))
    );
  }

  @Test
  @DisplayName("탈퇴하지 않은 사용자는 예약 삭제 시각이 없다")
  void activeUserHasNoScheduledDeletionAt() {
    UserListRequest request = new UserListRequest(
        null, null, null, null, null, 20, "ASCENDING", "email"
    );
    when(userRepository.findAllUsers(request)).thenReturn(
        new UserQueryPage(java.util.List.of(existingUser), null, null, false, 1)
    );

    CursorResponseUserDto result = service.getAllUsers(request);

    assertThat(result.data()).singleElement().satisfies(user -> {
      assertThat(user.deletedAt()).isNull();
      assertThat(user.scheduledDeletionAt()).isNull();
    });
  }

  @Test
  @DisplayName("일반 계정 탈퇴 시 비밀번호를 확인하고 세션을 무효화한다")
  void withdrawPasswordAccount() {
    existingUser.setPassword(encoder.encode(PASSWORD));
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(existingUser));

    service.withdraw(userId, PASSWORD);

    assertThat(existingUser.getDeletedAt()).isNotNull();
    verify(loginSessionStore).invalidateAll(userId);
  }

  @Test
  @DisplayName("현재 비밀번호가 다르면 탈퇴하지 않는다")
  void rejectWithdrawalWithInvalidPassword() {
    existingUser.setPassword(encoder.encode(PASSWORD));
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(existingUser));

    assertThatThrownBy(() -> service.withdraw(userId, "WrongPassword123!"))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD));

    assertThat(existingUser.getDeletedAt()).isNull();
    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("회원가입")
  void createUser() {
    Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", userId);
      ReflectionTestUtils.setField(saved, "createdAt", createdAt);
      return saved;
    });

    UserDto result = service.create(createRequest);

    verify(userRepository).save(assertArg(saved -> {
      assertThat(saved.getEmail()).isEqualTo(EMAIL);
      assertThat(saved.getUsername()).isEqualTo(USERNAME);
      assertThat(saved.getPassword()).isNotEqualTo(PASSWORD);
      assertThat(encoder.matches(PASSWORD, saved.getPassword())).isTrue();
      assertThat(saved.getRole()).isEqualTo(UserRole.USER);
      assertThat(saved.isLocked()).isFalse();
      assertThat(saved.getProfileImageUrl()).isNull();
      assertThat(saved.getDeletedAt()).isNull();
      assertThat(saved.getTempPassword()).isNull();
      assertThat(saved.getTempPasswordExpiresAt()).isNull();
    }));
    assertThat(result.id()).isEqualTo(userId);
    assertThat(result.email()).isEqualTo(EMAIL);
    assertThat(result.name()).isEqualTo(USERNAME);
    assertThat(result.role()).isEqualTo(UserRole.USER);
    assertThat(result.locked()).isFalse();
    assertThat(result.createdAt()).isEqualTo(createdAt);
  }

  @Test
  @DisplayName("이메일 중복. 저장 실패")
  void duplicateEmailDoesNotSave() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

    assertThatThrownBy(() -> service.create(createRequest))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("사용자 조회")
  void getUser() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

    UserDto result = service.getUser(userId);

    assertThat(result.id()).isEqualTo(userId);
    assertThat(result.email()).isEqualTo(EMAIL);
    assertThat(result.name()).isEqualTo(ORIGINAL_USERNAME);
    assertThat(result.profileImageUrl()).isEqualTo(ORIGINAL_IMAGE);
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("존재 하지 않는 사용자 조회")
  void missingUser() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getUser(userId))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})// 두번 실행하도록 valuesource 사용
  @DisplayName("이미지 없으면 이름만 수정. 기존 이미지를 유지한다")
  void preserveImageWhenAbsentOrEmpty(boolean emptyFile) {
    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

    MockMultipartFile image =
        emptyFile ? new MockMultipartFile("image", "empty.png", "image/png", new byte[0]) : null;

    UserDto result = service.updateUser(userId, userId, updateRequest, image);

    assertThat(existingUser.getUsername()).isEqualTo(CHANGED_USERNAME);
    assertThat(existingUser.getProfileImageUrl()).isEqualTo(ORIGINAL_IMAGE);
    assertThat(result.name()).isEqualTo(CHANGED_USERNAME);
    assertThat(result.profileImageUrl()).isEqualTo(ORIGINAL_IMAGE);
    assertUnchangedAccountFields(existingUser);
  }

  @Test
  @DisplayName("이미지 변경 프로필 수정")
  void updateNameAndImage() throws Exception {
    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

    MockMultipartFile image =
        new MockMultipartFile("image", "new.png", "image/png", new byte[]{1, 2, 3});
    String storedUrl = "/files/users/profiles/new.png";
    when(imageStorage.store(image, ImageCategory.USER_PROFILE)).thenReturn(storedUrl);

    UserDto result = service.updateUser(userId, userId, updateRequest, image);

    assertThat(existingUser.getUsername()).isEqualTo(CHANGED_USERNAME);
    assertThat(existingUser.getProfileImageUrl()).isEqualTo(storedUrl);
    assertThat(result.name()).isEqualTo(CHANGED_USERNAME);
    assertThat(result.profileImageUrl()).isEqualTo(storedUrl);
    assertUnchangedAccountFields(existingUser);
  }

  @Test
  @DisplayName("프로필 이미지 저장에 실패하면 사용자 정보를 변경하지 않는다")
  void imageStorageFailureDoesNotUpdateProfile() throws Exception {
    MockMultipartFile image = new MockMultipartFile(
        "image", "profile.png", "image/png", new byte[]{1}
    );
    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
    when(imageStorage.store(image, ImageCategory.USER_PROFILE))
        .thenThrow(new IOException("storage failure"));

    assertThatThrownBy(() -> service.updateUser(userId, userId, updateRequest, image))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_SAVE_FAILED));

    assertThat(existingUser.getUsername()).isEqualTo(ORIGINAL_USERNAME);
    assertThat(existingUser.getProfileImageUrl()).isEqualTo(ORIGINAL_IMAGE);
  }

  @Test
  @DisplayName("다른 사용자의 수정은 저장소 접근 전에 ACCESS_DENIED로 차단.")
  void nonOwnerCannotUpdate() {
    assertThatThrownBy(() -> service.updateUser(UUID.randomUUID(), userId,
        updateRequest, null))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));

    verifyNoInteractions(userRepository);
  }

  @Test
  @DisplayName("수정 요청자의 ID가 없으면 ACCESS_DENIED로 차단.")
  void missingActorCannotUpdate() {
    assertThatThrownBy(() -> service.updateUser(null, userId, updateRequest, null))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));

    verifyNoInteractions(userRepository);
  }

  @Test
  @DisplayName("본인 요청이어도 수정 대상이 없으면 USER_NOT_FOUND로 실패.")
  void missingUpdateTarget() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateUser(userId, userId, updateRequest, null))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("비밀번호 변경 시 임시 비밀번호를 제거하고 로그인 세션을 무효화한다")
  void updatePasswordClearsTemporaryPasswordAndInvalidatesSession() {
    String newPassword = "ChangedPassword123!";
    existingUser.issueTemporaryPassword(
        "temporary-password-hash",
        Instant.now().plusSeconds(180)
    );
    when(userRepository.findByIdForUpdate(userId))
        .thenReturn(Optional.of(existingUser));

    service.updatePassword(userId, userId, newPassword);

    assertThat(encoder.matches(newPassword, existingUser.getPassword())).isTrue();
    assertThat(existingUser.getTempPassword()).isNull();
    assertThat(existingUser.getTempPasswordExpiresAt()).isNull();
    verify(loginSessionStore).invalidateAll(userId);
  }

  @Test
  @DisplayName("관리자가 사용자 역할 변경시 로그인 세션 무효화.")
  void adminChangesRoleAndInvalidatesSession() {
    UUID adminId = UUID.randomUUID();
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(existingUser));

    service.updateRole(adminId, userId, UserRole.ADMIN);

    assertThat(existingUser.getRole()).isEqualTo(UserRole.ADMIN);

    verify(loginSessionStore).invalidateAll(userId);
  }

  @Test
  @DisplayName("관리자가 사용자를 잠그면 로그인 세션을 무효화")
  void adminLocksUserAndInvalidatesSession() {
    UUID adminId = UUID.randomUUID();
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(existingUser));

    service.updateLocked(adminId, userId, true);

    assertThat(existingUser.isLocked()).isTrue();

    verify(loginSessionStore).invalidateAll(userId);
  }

  @Test
  @DisplayName("관리자가 탈퇴 사용자의 연관 데이터를 지우고 영구 삭제한 뒤 로그인 세션을 무효화한다")
  void adminPurgesWithdrawnUserAndInvalidatesSession() {
    UUID adminId = UUID.randomUUID();
    existingUser.withdraw(Instant.now());
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(existingUser));

    service.purgeUser(adminId, userId);

    InOrder purgeOrder = inOrder(
        messageRepository,
        watchingSessionRepository,
        reviewRepository,
        userRepository,
        loginSessionStore
    );
    purgeOrder.verify(messageRepository).deleteAllByUserId(userId);
    purgeOrder.verify(watchingSessionRepository).deleteAllByWatcherId(userId);
    purgeOrder.verify(reviewRepository).deleteAllByAuthorId(userId);
    purgeOrder.verify(userRepository).delete(existingUser);
    purgeOrder.verify(userRepository).flush();
    purgeOrder.verify(loginSessionStore).invalidateAll(userId);
  }

  @Test
  @DisplayName("관리자는 탈퇴하지 않은 사용자를 영구 삭제할 수 없다")
  void adminCannotPurgeActiveUser() {
    UUID adminId = UUID.randomUUID();
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(existingUser));

    assertThatThrownBy(() -> service.purgeUser(adminId, userId))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_WITHDRAWN));

    verify(userRepository, never()).delete(any());
    verifyNoInteractions(messageRepository, watchingSessionRepository, reviewRepository);
    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("관리자는 자기 자신을 삭제할 수 없다")
  void adminCannotDeleteSelf() {
    assertThatThrownBy(() -> service.purgeUser(userId, userId))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));

    verifyNoInteractions(
        userRepository,
        messageRepository,
        watchingSessionRepository,
        reviewRepository,
        loginSessionStore
    );
  }

  @Test
  @DisplayName("관리자는 BOT 계정을 삭제할 수 없다")
  void adminCannotDeleteBot() {
    UUID adminId = UUID.randomUUID();
    User bot = User.createBot("bot@example.com", "bot");
    ReflectionTestUtils.setField(bot, "id", userId);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(bot));

    assertThatThrownBy(() -> service.purgeUser(adminId, userId))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));

    verifyNoInteractions(messageRepository, watchingSessionRepository, reviewRepository);
    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("관리자가 존재하지 않는 사용자를 수정시 USER_NOT_FOUND.")
  void adminCannotUpdateMissingUser() {
    UUID adminId = UUID.randomUUID();
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateLocked(adminId, userId, true))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("관리자는 자신의 역할 변경 불가.")
  void adminCannotChangeOwnRole() {
    assertThatThrownBy(() -> service.updateRole(userId, userId, UserRole.USER))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));

    verifyNoInteractions(userRepository, loginSessionStore);
  }

  @Test
  @DisplayName("관리자는 BOT 계정의 잠금 상태 변경 불가")
  void adminCannotLockBot() {
    UUID adminId = UUID.randomUUID();
    User bot = User.createBot("bot@example.com", "bot");
    ReflectionTestUtils.setField(bot, "id", userId);
    
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(bot));

    assertThatThrownBy(() -> service.updateLocked(adminId, userId, true))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));

    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("존재하지 않는 사용자는 탈퇴할 수 없다")
  void cannotWithdrawMissingUser() {
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.withdraw(userId, PASSWORD))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("이미 탈퇴한 사용자는 다시 탈퇴할 수 없다")
  void cannotWithdrawAlreadyWithdrawnUser() {
    existingUser.withdraw(Instant.now());
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(existingUser));

    assertThatThrownBy(() -> service.withdraw(userId, PASSWORD))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_ACCOUNT_WITHDRAWN));

    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("비밀번호가 없는 소셜 계정은 일반 탈퇴를 사용할 수 없다")
  void oauthUserCannotUsePasswordWithdrawal() {
    User oauthUser = User.createOAuth(EMAIL, USERNAME, null);
    ReflectionTestUtils.setField(oauthUser, "id", userId);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(oauthUser));

    assertThatThrownBy(() -> service.withdraw(userId, PASSWORD))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD));

    assertThat(oauthUser.getDeletedAt()).isNull();
    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("비밀번호 변경 대상이 없으면 실패한다")
  void cannotUpdatePasswordForMissingUser() {
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updatePassword(userId, userId, PASSWORD))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("BOT 역할은 사용자에게 부여할 수 없다")
  void cannotAssignBotRole() {
    UUID adminId = UUID.randomUUID();

    assertThatThrownBy(() -> service.updateRole(adminId, userId, UserRole.BOT))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

    verifyNoInteractions(userRepository, loginSessionStore);
  }

  @Test
  @DisplayName("동일한 역할 변경 요청은 세션을 무효화하지 않는다")
  void sameRoleUpdateIsNoOp() {
    UUID adminId = UUID.randomUUID();
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(existingUser));

    service.updateRole(adminId, userId, UserRole.USER);

    assertThat(existingUser.getRole()).isEqualTo(UserRole.USER);
    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("동일한 잠금 상태 변경 요청은 세션을 무효화하지 않는다")
  void sameLockUpdateIsNoOp() {
    UUID adminId = UUID.randomUUID();
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(existingUser));

    service.updateLocked(adminId, userId, false);

    assertThat(existingUser.isLocked()).isFalse();
    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("사용자 잠금 해제 시 기존 세션을 무효화하지 않는다")
  void unlockingUserDoesNotInvalidateSession() {
    UUID adminId = UUID.randomUUID();
    existingUser.changeLocked(true);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(existingUser));

    service.updateLocked(adminId, userId, false);

    assertThat(existingUser.isLocked()).isFalse();
    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("관리자가 존재하지 않는 사용자를 영구 삭제할 수 없다")
  void adminCannotPurgeMissingUser() {
    UUID adminId = UUID.randomUUID();
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.purgeUser(adminId, userId))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

    verifyNoInteractions(messageRepository, watchingSessionRepository, reviewRepository);
    verifyNoInteractions(loginSessionStore);
  }

  @Test
  @DisplayName("관리자 식별자가 없으면 사용자를 영구 삭제할 수 없다")
  void missingAdminCannotPurgeUser() {
    assertThatThrownBy(() -> service.purgeUser(null, userId))
        .isInstanceOfSatisfying(BaseException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED));

    verifyNoInteractions(
        userRepository,
        messageRepository,
        watchingSessionRepository,
        reviewRepository,
        loginSessionStore
    );
  }

  private void assertUnchangedAccountFields(User user) {
    assertThat(user.getId()).isEqualTo(userId);
    assertThat(user.getEmail()).isEqualTo(EMAIL);
    assertThat(user.getPassword()).isEqualTo(STORED_PASSWORD);
    assertThat(user.getRole()).isEqualTo(UserRole.USER);
    assertThat(user.isLocked()).isFalse();
    assertThat(user.getDeletedAt()).isNull();
    assertThat(user.getTempPassword()).isNull();
    assertThat(user.getTempPasswordExpiresAt()).isNull();
  }
}
