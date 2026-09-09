package com.codeit.modoo_playlist.moduleapi.domain.user.controller;

import com.codeit.modoo_playlist.moduleapi.dto.jwt.JwtDto;
import com.codeit.modoo_playlist.moduleapi.dto.request.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
public class AuthController {

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
      @CookieValue("REFRESH_TOKEN") String refreshToken,
      HttpServletResponse response
  ) {
    // TODO: 토큰 재발급 및 새 REFRESH_TOKEN 쿠키 설정
    throw new UnsupportedOperationException("구현 예정");
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
