package com.codeit.modoo_playlist.moduleapi.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtLogoutHandler implements LogoutHandler {

  private final JwtTokenProvider tokenProvider;
  private final LoginSessionStore loginSessionStore;

  //  로그아웃은 sid가 달라 세션 무효화가 되거나 refresh 토큰이 없는 등의 문제가 생겨도
//  쿠키를 제거하고 종료.
  @Override
  public void logout(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) {

    Cookie expirationCookie = tokenProvider.generateRefreshTokenExpirationCookie();

    response.addCookie(expirationCookie);

//    refresh 쿠키가 있는 경우
//    refresh 쿠키가 없지만 엑세스 토큰이 있는 경우 제거
//    그 외에는 204로 쿠키만 제거.
    Optional<SessionIdentity> sessionIdentity =
        findRefreshToken(request)
            .filter(tokenProvider::validateRefreshToken)
            .map(token -> new SessionIdentity(
                tokenProvider.getUserId(token),
                tokenProvider.getSid(token)
            ))
            .or(() -> findAccessToken(request)
                .filter(tokenProvider::validateAccessToken)
                .map(token -> new SessionIdentity(
                    tokenProvider.getUserId(token),
                    tokenProvider.getSid(token)
                )));

    sessionIdentity.ifPresent(identity ->
        loginSessionStore.invalidate(
            identity.userId(),
            identity.sid()
        )
    );

    log.debug("JWT logout handler executed - refresh token cookie cleared");
  }

  //  쿠키에서 refresh_token 값 찾기
  private Optional<String> findRefreshToken(
      HttpServletRequest request
  ) {
    Cookie[] cookies = request.getCookies();

    if (cookies == null) {
      return Optional.empty();
    }

    return Arrays.stream(cookies)
        .filter(cookie ->
            JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME
                .equals(cookie.getName())
        )
        .map(Cookie::getValue)
        .filter(StringUtils::hasText)
        .findFirst();
  }

  private Optional<String> findAccessToken(
      HttpServletRequest request
  ) {
    String authorization = request.getHeader(
        HttpHeaders.AUTHORIZATION
    );

    if (!StringUtils.hasText(authorization)) {
      return Optional.empty();
    }

    String bearerPrefix = "Bearer ";

    if (!authorization.startsWith(bearerPrefix)) {
      return Optional.empty();
    }

    String accessToken = authorization.substring(
        bearerPrefix.length()
    );

    return StringUtils.hasText(accessToken)
        ? Optional.of(accessToken)
        : Optional.empty();
  }

  //  이 파일에서만 사용하니까 private record로 사용
  private record SessionIdentity(
      UUID userId,
      UUID sid
  ) {

  }
}
