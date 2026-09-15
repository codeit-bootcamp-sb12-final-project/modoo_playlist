package com.codeit.modoo_playlist.moduleapi.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.UserServiceImpl;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.request.UserProfileUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
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
    service = new UserServiceImpl(userRepository, encoder, Mappers.getMapper(UserMapper.class));
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
  void updateNameAndImage() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

    MockMultipartFile image =
        new MockMultipartFile("image", "new.png", "image/png", new byte[]{1, 2, 3});

    UserDto result = service.updateUser(userId, userId, updateRequest, image);

    assertThat(existingUser.getUsername()).isEqualTo(CHANGED_USERNAME);
    assertThat(existingUser.getProfileImageUrl()).isEqualTo("new.png");
    assertThat(result.name()).isEqualTo(CHANGED_USERNAME);
    assertThat(result.profileImageUrl()).isEqualTo("new.png");
    assertUnchangedAccountFields(existingUser);
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
