package com.codeit.modoo_playlist.moduleapi.domain.user;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.controller.AuthController;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.AuthService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.TokenRefreshResult;
import com.codeit.modoo_playlist.moduleapi.exception.GlobalExceptionHandler;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  private static final String EMAIL = "user@example.com";
  private static final String USERNAME = "tester";
  private static final String OLD_REFRESH_TOKEN = "old-refresh";
  private static final String NEW_REFRESH_TOKEN = "new-refresh";
  private static final String NEW_ACCESS_TOKEN = "new-access";

  @Mock
  AuthService authService;

  @Mock
  JwtTokenProvider tokens;

  private MockMvc mvc;
  private UserDto user;
  private Instant expiresAt;
  private Cookie oldRefreshCookie;
  private Cookie newRefreshCookie;

  @BeforeEach
  void setUp() {
    user = new UserDto(UUID.randomUUID(), EMAIL, USERNAME, null,
        UserRole.USER, false, Instant.now());
    expiresAt = Instant.now().plusSeconds(600);
    oldRefreshCookie = new Cookie("REFRESH_TOKEN", OLD_REFRESH_TOKEN);
    newRefreshCookie = new Cookie("REFRESH_TOKEN", NEW_REFRESH_TOKEN);
    newRefreshCookie.setHttpOnly(true);
    mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, tokens))
        .setControllerAdvice(new GlobalExceptionHandler()).build();
  }

  @Test
  @DisplayName("refresh 쿠키를 서비스에 전달. 새 쿠키와 access DTO를 반환.")
  void refresh() throws Exception {
    when(authService.refresh(OLD_REFRESH_TOKEN)).thenReturn(
        new TokenRefreshResult(user, NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN, expiresAt));
    when(tokens.generateRefreshTokenCookie(NEW_REFRESH_TOKEN, expiresAt)).thenReturn(
        newRefreshCookie);

    mvc.perform(post("/api/auth/refresh").cookie(oldRefreshCookie))
        .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(cookie().value("REFRESH_TOKEN", NEW_REFRESH_TOKEN))
        .andExpect(cookie().httpOnly("REFRESH_TOKEN", true))
        .andExpect(jsonPath("$.accessToken").value(NEW_ACCESS_TOKEN))
        .andExpect(jsonPath("$.userDto.id").value(user.id().toString()))
        .andExpect(jsonPath("$.refreshToken").doesNotExist());

    verify(authService).refresh(OLD_REFRESH_TOKEN);
  }

  @Test
  @DisplayName("쿠키가 없으면 서비스에 null을 전달.INVALID_TOKEN으로 응답.")
  void missingCookie() throws Exception {
    when(authService.refresh(null)).thenThrow(new BaseException(ErrorCode.INVALID_TOKEN));

    mvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
        .andExpect(cookie().doesNotExist("REFRESH_TOKEN"));

    verify(authService).refresh(null);
    verifyNoInteractions(tokens);
  }

  @ParameterizedTest
  @EnumSource(value = ErrorCode.class, names = {"INVALID_TOKEN", "LOGIN_SESSION_INVALIDATED",
      "TOKEN_GENERATION_FAILED", "LOGIN_SESSION_UPDATE_FAILED",
      "AUTHENTICATION_SERVICE_UNAVAILABLE"})
  @DisplayName("서비스 예외의 ErrorCode와 HTTP 상태를 전달. 성공 쿠키를 발급하지 않는다.")
  void refreshErrorResponse(ErrorCode code) throws Exception {
    when(authService.refresh(OLD_REFRESH_TOKEN)).thenThrow(new BaseException(code));
    
    mvc.perform(post("/api/auth/refresh").cookie(oldRefreshCookie))
        .andExpect(status().is(code.getStatus())).andExpect(jsonPath("$.code").value(code.name()))
        .andExpect(jsonPath("$.message").value(code.getMessage()))
        .andExpect(jsonPath("$.status").value(code.getStatus()))
        .andExpect(cookie().doesNotExist("REFRESH_TOKEN"))
        .andExpect(jsonPath("$.accessToken").doesNotExist());

    verifyNoInteractions(tokens);
  }
}
