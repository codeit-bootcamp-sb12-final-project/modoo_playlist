package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.core.global.exception.ErrorResponse;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final JsonMapper objectMapper;//spring4는 jackson3으로 구성하는 듯?
    private final JwtRegistry<UUID> jwtRegistry;
    private final RoleHierarchy roleHierarchy;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/api/auth/sign-in")
                || uri.equals("/api/auth/refresh")
                || uri.equals("/api/auth/csrf-token");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = resolveToken(request);

            if (StringUtils.hasText(token)) {
                if (tokenProvider.validateAccessToken(token)
                        && jwtRegistry.hasActiveJwtInformationByAccessToken(token)) {
                    String username = tokenProvider.getUsernameFromToken(token);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    roleHierarchy.getReachableGrantedAuthorities(userDetails.getAuthorities())
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Set authentication for user: {}", username);
                } else {
                    log.debug("Invalid JWT token");
                    sendErrorResponse(response, "Invalid JWT token", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }
        } catch (Exception e) {
            log.debug("JWT authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            sendErrorResponse(response, "JWT authentication failed", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

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
