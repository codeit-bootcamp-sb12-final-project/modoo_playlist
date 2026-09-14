package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.core.global.exception.ErrorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginSession;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.codeit.modoo_playlist.moduleapi.security.UserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;
  private final UserDetailsService userDetailsService;
  private final JsonMapper objectMapper;//spring4는 jackson3으로 구성하는 듯?
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
        throw new BadCredentialsException("유효하지 않은 access 토큰입니다.");
      }

//      토큰에서 정보 추출
      UUID sid = tokenProvider.getSid(token);
      UUID tokenUserId = tokenProvider.getUserId(token);

//      로그인 세션이 활성화 중인지 체크
      Optional<LoginSession> sessionOptional =
          loginSessionStore.findActive(tokenUserId, sid);

      if (sessionOptional.isEmpty()) {
        throw new BadCredentialsException("유효한 로그인 세션이 없습니다.");
      }

      LoginSession session = sessionOptional.get();

//      토큰 유저와 세션 유저 비교
      if (!tokenUserId.equals(session.userId())) {
        throw new BadCredentialsException("로그인 사용자 정보가 일치하지 않습니다.");
      }

      String email = tokenProvider.getUsernameFromToken(token);

      UserDetails userDetails = userDetailsService.loadUserByUsername(email);

//      DB에서 조회한 사용자 ID까지 비교
      if (!tokenUserId.equals(userDetails.getUserDto().id())) {
        throw new BadCredentialsException("로그인 사용자 정보가 일치하지 않습니다.");
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

    } catch (AuthenticationException e) {
      SecurityContextHolder.clearContext();

      sendErrorResponse(
          response,
          "인증 정보가 유효하지 않습니다.",
          HttpServletResponse.SC_UNAUTHORIZED
      );
      return;

    } catch (DataAccessException e) {
      SecurityContextHolder.clearContext();
      log.error("Authentication storage access failed", e);

      sendErrorResponse(
          response,
          "인증 저장소에 접근할 수 없습니다.",
          HttpServletResponse.SC_SERVICE_UNAVAILABLE
      );
      return;

    } catch (Exception e) {
      SecurityContextHolder.clearContext();
      log.error("Unexpected JWT authentication failure", e);

      sendErrorResponse(
          response,
          "인증 처리 중 오류가 발생했습니다.",
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR
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

  private void sendErrorResponse(HttpServletResponse response, String message, int status)
      throws IOException {
    ErrorResponse errorResponse = new ErrorResponse(new RuntimeException(message), status);

    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    String jsonResponse = objectMapper.writeValueAsString(errorResponse);
    response.getWriter().write(jsonResponse);
  }
}
