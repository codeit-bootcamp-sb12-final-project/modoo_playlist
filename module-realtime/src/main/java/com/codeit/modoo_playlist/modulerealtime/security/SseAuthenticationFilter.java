package com.codeit.modoo_playlist.modulerealtime.security;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class SseAuthenticationFilter extends OncePerRequestFilter {

    private final RealtimeAuthenticationService authenticationService;

    @Override
    protected boolean shouldNotFilter (HttpServletRequest request){
        // SSE 요청에만 적용
        return !"GET".equals(request.getMethod())
                || !"/api/sse".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try{
            String token = resolveToken(request);

            if(token == null || token.isBlank()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            var authentication = authenticationService.authenticate(token);

            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } catch(BaseException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(exception.getErrorCode().getStatus());
            return;
        }

        filterChain.doFilter(request,response);
    }

    private String resolveToken(HttpServletRequest request){
        String authorization = request.getHeader("Authorization");

        if(authorization != null){
            if(!authorization.startsWith("Bearer ")){
                throw new BaseException(ErrorCode.INVALID_CSRF_TOKEN);
            }

            return authorization.substring(7);
        }

        return request.getParameter("access_token");
    }
}
