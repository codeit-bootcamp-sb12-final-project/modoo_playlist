package com.codeit.modoo_playlist.moduleapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationProviderTest {

  private static final String EMAIL = "user@example.com";
  private static final String PERMANENT_PASSWORD = "Permanent123!";
  private static final String TEMPORARY_PASSWORD = "temporary1!!";
  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  @Mock
  UserRepository userRepository;

  @Mock
  UserMapper userMapper;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private UserAuthenticationProvider provider;
  private User user;
  private UserDto userDto;

  @BeforeEach
  void setUp() {
    UUID userId = UUID.randomUUID();
    user = User.create(
        EMAIL,
        "tester",
        passwordEncoder.encode(PERMANENT_PASSWORD)
    );
    ReflectionTestUtils.setField(user, "id", userId);
    user.issueTemporaryPassword(
        passwordEncoder.encode(TEMPORARY_PASSWORD),
        NOW.plusSeconds(180)
    );

    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    userDto = new UserDto(
        userId,
        EMAIL,
        "tester",
        null,
        UserRole.USER,
        false,
        NOW
    );

    provider = new UserAuthenticationProvider(
        userRepository,
        userMapper,
        passwordEncoder,
        Clock.fixed(NOW, ZoneOffset.UTC)
    );
  }

  @Test
  @DisplayName("활성 임시 비밀번호로 로그인하면 TEMPORARY 인증이 된다")
  void authenticatesWithActiveTemporaryPassword() {
    when(userMapper.toDto(user)).thenReturn(userDto);

    Authentication result = provider.authenticate(login(TEMPORARY_PASSWORD));

    UserDetails principal = (UserDetails) result.getPrincipal();
    assertThat(result.isAuthenticated()).isTrue();
    assertThat(principal.getCredentialType())
        .isEqualTo(LoginCredentialType.TEMPORARY);
  }

  @Test
  @DisplayName("임시 비밀번호 활성 중에는 기존 비밀번호 로그인을 거부한다")
  void rejectsPermanentPasswordWhileTemporaryPasswordIsActive() {
    assertThatThrownBy(() -> provider.authenticate(login(PERMANENT_PASSWORD)))
        .isInstanceOfSatisfying(
            CodedAuthenticationException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.TEMP_PASSWORD_ACTIVE)
        );
  }

  @Test
  @DisplayName("임시 비밀번호 만료 후에는 기존 비밀번호만 허용한다")
  void allowsPermanentPasswordAfterTemporaryPasswordExpires() {
    when(userMapper.toDto(user)).thenReturn(userDto);
    user.issueTemporaryPassword(
        passwordEncoder.encode(TEMPORARY_PASSWORD),
        NOW.minusSeconds(1)
    );

    Authentication result = provider.authenticate(login(PERMANENT_PASSWORD));
    UserDetails principal = (UserDetails) result.getPrincipal();

    assertThat(principal.getCredentialType())
        .isEqualTo(LoginCredentialType.PERMANENT);
    assertThatThrownBy(() -> provider.authenticate(login(TEMPORARY_PASSWORD)))
        .isInstanceOfSatisfying(
            CodedAuthenticationException.class,
            exception -> assertThat(exception.getErrorCode())
                .isEqualTo(ErrorCode.TEMP_PASSWORD_EXPIRED)
        );
  }

  private Authentication login(String password) {
    return UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, password);
  }
}
