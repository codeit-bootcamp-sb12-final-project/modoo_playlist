package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginSession;
import com.codeit.modoo_playlist.moduleapi.security.SecurityErrorResponseWriter;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.codeit.modoo_playlist.moduleapi.security.UserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;
  private final UserDetailsService userDetailsService;
  private final SecurityErrorResponseWriter errorResponseWriter;
  private final LoginSessionStore loginSessionStore;
  private final RoleHierarchy roleHierarchy;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri.equals("/api/auth/sign-in")
        || uri.equals("/api/auth/refresh")
        || uri.equals("/api/auth/csrf-token");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String token = resolveToken(request);
//    사용자 판단을 다음 세개로 확인
//    토큰 user == 세션 user == DB에서 조회한 사용자

//    토큰이 없는 요청은 뒤에 인가 파트에서 판단
//    공개 api -> 인증 없이 가능
//    인증 필요 -> 인증 없으면 거부
    if (!StringUtils.hasText(token)) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
//      엑세스인지 우선 판단
      if (!tokenProvider.validateAccessToken(token)) {
        throw new BaseException(ErrorCode.INVALID_TOKEN);
      }

//      토큰에서 정보 추출
      UUID sid = tokenProvider.getSid(token);
      UUID tokenUserId = tokenProvider.getUserId(token);

//      로그인 세션이 활성화 중인지 체크
//      해당 메서드에서 사용자까지 비교함.
      LoginSession session = loginSessionStore.findActive(tokenUserId, sid)
          .orElseThrow(() -> new BaseException(ErrorCode.LOGIN_SESSION_INVALIDATED));

      String email = tokenProvider.getUsernameFromToken(token);

      UserDetails userDetails;

      try {
        userDetails = userDetailsService.loadUserByUsername(email);
      } catch (UsernameNotFoundException e) {
        throw new BaseException(ErrorCode.INVALID_TOKEN, e);
      }

//      DB에서 조회한 사용자 ID까지 비교
      if (!tokenUserId.equals(userDetails.getUserDto().id())) {
        throw new BaseException(ErrorCode.INVALID_TOKEN);
      }

//      유저가 잠금일 경우 막는 방어 코드
      if (!userDetails.isAccountNonLocked()) {
        throw new BaseException(ErrorCode.USER_ACCOUNT_LOCKED);
      }

      // 7. 현재 요청의 인증 정보 구성
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              userDetails,
              null,
              roleHierarchy.getReachableGrantedAuthorities(
                  userDetails.getAuthorities()
              )
          );

      authentication.setDetails(
          new WebAuthenticationDetailsSource().buildDetails(request)
      );

      SecurityContextHolder.getContext().setAuthentication(authentication);

    } catch (BaseException e) {
      SecurityContextHolder.clearContext();
      errorResponseWriter.write(response, e);
      return;

    } catch (DataAccessException e) {
      SecurityContextHolder.clearContext();
      log.error("Authentication storage access failed", e);

      errorResponseWriter.write(
          response,
          ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE
      );
      return;

    } catch (Exception e) {
      SecurityContextHolder.clearContext();
      log.error("Unexpected JWT authentication failure", e);

      errorResponseWriter.write(
          response,
          ErrorCode.INTERNAL_SERVER_ERROR
      );
      return;
    }

    // 인증을 완료했으므로 다음 필터로 진행
    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }

    // 브라우저 기본 EventSource는 Authorization 헤더를 지정할 수 없다.
    // 토큰 노출 범위를 줄이기 위해 SSE 구독 엔드포인트에서만 쿼리 토큰을 허용한다.
    if ("/api/sse".equals(request.getRequestURI())) {
      String queryToken = request.getParameter("access_token");
      if (StringUtils.hasText(queryToken)) {
        return queryToken;
      }
    }
    return null;
  }
}
