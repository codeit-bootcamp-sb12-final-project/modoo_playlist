package com.codeit.modoo_playlist.moduleapi.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.event.TemporaryPasswordIssuedEvent;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.TemporaryPasswordGenerator;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.AuthServiceImpl;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginIssueResult;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginSession;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.TokenRefreshResult;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import com.codeit.modoo_playlist.moduleapi.security.LoginCredentialType;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.codeit.modoo_playlist.moduleapi.security.UserDetailsService;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import com.codeit.modoo_playlist.moduleapi.security.jwt.LoginSessionStore;
import com.codeit.modoo_playlist.moduleapi.security.jwt.RefreshTokenHasher;
import com.nimbusds.jose.JOSEException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final String EMAIL = "test@example.com";
  private static final String OLD_REFRESH_TOKEN = "old";
  private static final String NEW_ACCESS_TOKEN = "new-access";
  private static final String NEW_REFRESH_TOKEN = "new-refresh";
  private static final String TEMPORARY_PASSWORD = "temporary1!!";
  private static final String ENCODED_TEMPORARY_PASSWORD = "encoded-temporary-password";
  private static final long REFRESH_EXPIRATION_MS = 3_600_000L;
  private static final long MAX_REFRESH_EXPIRATION_MS = 86_400_000L;

  @Mock
  JwtTokenProvider tokens;

  @Mock
  LoginSessionStore sessions;

  @Mock
  UserDetailsService users;

  @Mock
  UserRepository userRepository;

  @Mock
  PasswordEncoder passwordEncoder;

  @Mock
  TemporaryPasswordGenerator temporaryPasswordGenerator;

  @Mock
  ApplicationEventPublisher eventPublisher;

  @Mock
  UserMapper userMapper;

  private final RefreshTokenHasher hasher = new RefreshTokenHasher();
  private final Clock clock = Clock.systemUTC();
  private UUID userId;
  private UUID sid;
  private AuthServiceImpl authService;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    sid = UUID.randomUUID();
    authService = new AuthServiceImpl(
        tokens,
        sessions,
        users,
        hasher,
        userRepository,
        passwordEncoder,
        temporaryPasswordGenerator,
        eventPublisher,
        clock,
        userMapper
    );
    ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", REFRESH_EXPIRATION_MS);
    ReflectionTestUtils.setField(authService, "maxRefreshTokenExpirationMs",
        MAX_REFRESH_EXPIRATION_MS);
    ReflectionTestUtils.setField(
        authService,
        "temporaryPasswordExpiration",
        Duration.ofMinutes(3)
    );
  }

  @Test
  @DisplayName("비밀번호 초기화 시 임시 비밀번호를 저장하고 이메일을 전송한다")
  void resetPasswordIssuesAndSendsTemporaryPassword() {
    User user = user();
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    when(temporaryPasswordGenerator.generate()).thenReturn(TEMPORARY_PASSWORD);
    when(passwordEncoder.encode(TEMPORARY_PASSWORD))
        .thenReturn(ENCODED_TEMPORARY_PASSWORD);

    authService.resetPassword(EMAIL);

    assertThat(user.getTempPassword()).isEqualTo(ENCODED_TEMPORARY_PASSWORD);
    assertThat(user.hasActiveTemporaryPassword(clock.instant())).isTrue();
    verify(userRepository).saveAndFlush(user);
    verify(eventPublisher).publishEvent(
        org.mockito.Mockito.<Object>assertArg(event -> {
          assertThat(event).isInstanceOf(TemporaryPasswordIssuedEvent.class);
          TemporaryPasswordIssuedEvent issuedEvent = (TemporaryPasswordIssuedEvent) event;

          assertThat(issuedEvent.userId()).isEqualTo(userId);
          assertThat(issuedEvent.email()).isEqualTo(EMAIL);
          assertThat(issuedEvent.temporaryPassword()).isEqualTo(TEMPORARY_PASSWORD);
          assertThat(issuedEvent.expiresAt())
              .isEqualTo(user.getTempPasswordExpiresAt());
        })
    );
  }

  @Test
  @DisplayName("임시 로그인은 접근 토큰만 발급하고 갱신 토큰은 생성하지 않는다")
  void temporaryLoginDoesNotIssueRefreshToken() throws Exception {
    User user = user();
    user.issueTemporaryPassword(
        ENCODED_TEMPORARY_PASSWORD,
        clock.instant().plusSeconds(180)
    );
    UserDto userDto = details(userId).getUserDto();

    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(userDto);
    when(tokens.generateAccessToken(any(), any(), any()))
        .thenReturn(NEW_ACCESS_TOKEN);

    LoginIssueResult result =
        authService.issueLogin(userId, LoginCredentialType.TEMPORARY);

    assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);
    assertThat(result.refreshToken()).isNull();
    verify(tokens, never()).generateRefreshToken(any(), any(), any());
    verify(sessions).register(assertArg(session ->
        assertThat(session.refreshTokenHash()).isEmpty()
    ));
  }

  @Test
  @DisplayName("탈퇴한 계정은 새 로그인 세션을 발급하지 않는다")
  void withdrawnAccountCannotLogin() {
    User user = user();
    user.withdraw(clock.instant());
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));

    assertError(
        () -> authService.issueLogin(userId, LoginCredentialType.PERMANENT),
        ErrorCode.USER_ACCOUNT_WITHDRAWN
    );

    verifyNoInteractions(tokens, sessions, userMapper);
  }

  @Test
  @DisplayName("탈퇴한 계정의 비밀번호 초기화 요청은 무시한다")
  void withdrawnAccountCannotResetPassword() {
    User user = user();
    user.withdraw(clock.instant());
    when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

    authService.resetPassword(EMAIL);

    verifyNoInteractions(passwordEncoder, temporaryPasswordGenerator, eventPublisher);
    verify(userRepository, never()).saveAndFlush(any());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  @DisplayName("빈 refresh 토큰은 INVALID_TOKEN으로 처리.")
  void missingToken(String token) {
    assertError(() -> authService.refresh(token), ErrorCode.INVALID_TOKEN);

    verifyNoInteractions(tokens, sessions, users);
  }

  @Test
  @DisplayName("토큰 검증시 실패 반환.")
  void invalidToken() {
    when(tokens.validateRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(false);

    assertError(() -> authService.refresh(OLD_REFRESH_TOKEN), ErrorCode.INVALID_TOKEN);

    verifyNoInteractions(sessions, users);
  }

  @Test
  @DisplayName("redis에 sid가 없으면 LOGIN_SESSION_INVALIDATED 예외")
  void invalidatedSession() {
    validToken();

    when(sessions.findActive(userId, sid)).thenReturn(Optional.empty());

    assertError(() -> authService.refresh(OLD_REFRESH_TOKEN), ErrorCode.LOGIN_SESSION_INVALIDATED);

    verifyNoInteractions(users);
  }

  @Test
  @DisplayName("토큰과 저장된 세션의 사용자 불일치 시 실패.")
  void sessionIdentityMismatch() {
    validToken();

    when(sessions.findActive(userId, sid)).thenReturn(
        Optional.of(session(UUID.randomUUID(), Instant.now())));

    assertError(() -> authService.refresh(OLD_REFRESH_TOKEN), ErrorCode.INVALID_TOKEN);

    verify(sessions, never()).rotateRefreshToken(any(), any(), any(), any(), any());
    verifyNoInteractions(users);
  }

  @Test
  @DisplayName("토큰과 DB 사용자의 ID가 다르면 갱신실패.")
  void databaseIdentityMismatch() {
    validToken();

    when(sessions.findActive(userId, sid)).thenReturn(Optional.of(session(userId, Instant.now())));
    when(users.loadUserByUsername(EMAIL)).thenReturn(details(UUID.randomUUID()));

    assertError(() -> authService.refresh(OLD_REFRESH_TOKEN), ErrorCode.INVALID_TOKEN);

    verify(sessions, never()).rotateRefreshToken(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("갱신해도 최초 로그인 기준 최대 유지 기간이 정해짐.")
  void capsAbsoluteSessionLifetime() throws Exception {
    Instant created = Instant.now().minusSeconds(86_100).truncatedTo(ChronoUnit.SECONDS);
    prepareRefresh(created);

    when(tokens.generateAccessToken(any(), eq(sid), any())).thenReturn(NEW_ACCESS_TOKEN);
    when(tokens.generateRefreshToken(any(), eq(sid), any())).thenReturn(NEW_REFRESH_TOKEN);

    TokenRefreshResult result = authService.refresh(OLD_REFRESH_TOKEN);

    assertThat(result.expiresAt()).isEqualTo(created.plusSeconds(86_400));
    assertThat(result.accessToken()).isEqualTo(NEW_ACCESS_TOKEN);

    verify(sessions).rotateRefreshToken(userId, sid, hasher.hash(OLD_REFRESH_TOKEN),
        hasher.hash(NEW_REFRESH_TOKEN), result.expiresAt());
  }

  @Test
  @DisplayName("토큰 생성 실패는 TOKEN_GENERATION_FAILED이며 세션은 교체 x.")
  void tokenGenerationFailure() throws Exception {
    prepareRefresh(Instant.now());

    when(tokens.generateAccessToken(any(), any(), any())).thenThrow(
        new JOSEException("test failure"));

    assertError(() -> authService.refresh(OLD_REFRESH_TOKEN), ErrorCode.TOKEN_GENERATION_FAILED);

    verify(sessions, never()).rotateRefreshToken(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("원자적 토큰 교체에 실패하면 새 토큰을 결과로 반환하지 않는다")
  void rotationFailure() throws Exception {
    prepareRefresh(Instant.now());

    when(tokens.generateAccessToken(any(), any(), any())).thenReturn(NEW_ACCESS_TOKEN);
    when(tokens.generateRefreshToken(any(), any(), any())).thenReturn(NEW_REFRESH_TOKEN);

    doThrow(new BaseException(ErrorCode.INVALID_TOKEN)).when(sessions)
        .rotateRefreshToken(any(), any(), any(), any(), any());

    assertError(() -> authService.refresh(OLD_REFRESH_TOKEN), ErrorCode.INVALID_TOKEN);
  }

  private void validToken() {
    when(tokens.validateRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
    when(tokens.getSid(OLD_REFRESH_TOKEN)).thenReturn(sid);
    when(tokens.getUserId(OLD_REFRESH_TOKEN)).thenReturn(userId);
    when(tokens.getUsernameFromToken(OLD_REFRESH_TOKEN)).thenReturn(EMAIL);
  }

  private void prepareRefresh(Instant created) {
    validToken();
    when(sessions.findActive(userId, sid)).thenReturn(Optional.of(session(userId, created)));
    when(users.loadUserByUsername(EMAIL)).thenReturn(details(userId));
  }

  private LoginSession session(UUID id, Instant created) {
    return new LoginSession(sid, id, hasher.hash(OLD_REFRESH_TOKEN), created,
        Instant.now().plusSeconds(600));
  }

  private UserDetails details(UUID id) {
    return new UserDetails(new UserDto(id, EMAIL, "test", null, UserRole.USER, false,
        Instant.now()), "hash");
  }

  private User user() {
    User user = User.create(EMAIL, "test", "encoded-password");
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }

  private void assertError(Runnable action, ErrorCode code) {
    assertThatThrownBy(action::run).isInstanceOfSatisfying(BaseException.class,
        e -> assertThat(e.getErrorCode()).isEqualTo(code));
  }
}
