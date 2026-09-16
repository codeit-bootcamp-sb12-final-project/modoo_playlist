package com.codeit.modoo_playlist.moduleapi.security;

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
import com.codeit.modoo_playlist.moduleapi.config.properties.AdminProperties;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

  private static final String EMAIL = "admin@example.com";
  private static final String PASSWORD = "Password123!";
  private static final String NAME = "admin";

  @Mock
  UserRepository userRepository;

  @Mock
  PasswordEncoder passwordEncoder;

  @Test
  @DisplayName("관리자 == 0 새로 만듬")
  void createsAdminWhenMissing() {
    when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
    when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

    initializer(properties(EMAIL, PASSWORD)).run(null);

    verify(userRepository).save(assertArg(admin -> {
      assertThat(admin.getEmail()).isEqualTo(EMAIL);
      assertThat(admin.getUsername()).isEqualTo(NAME);
      assertThat(admin.getPassword()).isEqualTo("encoded-password");
      assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
      assertThat(admin.isLocked()).isFalse();
    }));
  }

  @Test
  @DisplayName("관리자>=1 초기화 생략")
  void skipsWhenAdminExists() {
    when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

    initializer(properties(EMAIL, PASSWORD)).run(null);

    verify(userRepository, never()).save(any());
    verifyNoInteractions(passwordEncoder);
  }

  @Test
  @DisplayName("관리자 == 0일때 설정 없으면 초기화 실패.")
  void rejectsInvalidConfiguration() {
    when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

    assertError(
        initializer(properties(EMAIL, " ")),
        ErrorCode.ADMIN_CONFIGURATION_INVALID);
  }

  @Test
  @DisplayName("관리자 이메일을 일반 사용자가 쓰고 있으면 초기화 실패.")
  void rejectsEmailConflict() {
    when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
    when(userRepository.findByEmail(EMAIL))
        .thenReturn(Optional.of(User.create(EMAIL, "user", "encoded")));

    assertError(
        initializer(properties(EMAIL, PASSWORD)),
        ErrorCode.ADMIN_EMAIL_CONFLICT);
    
    verify(userRepository, never()).save(any());
  }

  private AdminProperties properties(String email, String password) {
    return new AdminProperties(true, email, password, NAME);
  }

  private AdminInitializer initializer(AdminProperties properties) {
    return new AdminInitializer(properties, userRepository, passwordEncoder);
  }

  private void assertError(AdminInitializer initializer, ErrorCode errorCode) {
    assertThatThrownBy(() -> initializer.run(null))
        .isInstanceOfSatisfying(BaseException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
  }
}
