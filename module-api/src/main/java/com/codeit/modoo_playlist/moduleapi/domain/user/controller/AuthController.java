package com.codeit.modoo_playlist.moduleapi.domain.user.controller;

import com.codeit.modoo_playlist.moduleapi.domain.user.service.AuthService;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.JwtDto;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.TokenRefreshResult;
import com.codeit.modoo_playlist.moduleapi.dto.request.ResetPasswordRequest;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final JwtTokenProvider jwtTokenProvider;

//Sign-in과 Sign-out은 필터체인에서 처리.

  @PostMapping(
      name = "비밀번호 초기화",
      path = "/reset-password"
  )
  public ResponseEntity<Void> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request
  ) {
    // TODO: 임시 비밀번호 발급 및 이메일 전송
    throw new UnsupportedOperationException("구현 예정");
  }

  @PostMapping(
      name = "토큰 재발급",
      path = "/refresh"
  )
  public ResponseEntity<JwtDto> reissueToken(
      @CookieValue(
          value = JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME,
          required = false
      ) String refreshToken,
      HttpServletResponse response
  ) {
    TokenRefreshResult tokenRefreshResult = authService.refresh(refreshToken);

    Cookie refreshCookie = jwtTokenProvider.generateRefreshTokenCookie(
        tokenRefreshResult.refreshToken(),
        tokenRefreshResult.expiresAt()
    );

    JwtDto jwtDto = new JwtDto(
        tokenRefreshResult.userDto(),
        tokenRefreshResult.accessToken()
    );

    response.addCookie(refreshCookie);

//    인증 응답을 브라우저나 캐시가 저장하지 않도록 여기에도 명시.
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(jwtDto);
  }

  @GetMapping(
      name = "CSRF 토큰 조회",
      path = "/csrf-token"
  )
  public ResponseEntity<Void> getCsrfToken(
      CsrfToken csrfToken
  ) {
    csrfToken.getToken();
    return ResponseEntity.noContent().build();
  }
}
