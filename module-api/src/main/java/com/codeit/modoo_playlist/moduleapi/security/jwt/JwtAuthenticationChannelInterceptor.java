package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.codeit.modoo_playlist.moduleapi.security.UserDetailsService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationChannelInterceptor implements ChannelInterceptor {

  private final JwtTokenProvider tokenProvider;
  private final RoleHierarchy roleHierarchy;
  private final LoginSessionStore loginSessionStore;
  private final UserDetailsService userDetailsService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    // STOMP 메시지가 아닌 경우와 CONNECT 이외의 프레임은 그대로 통과
    if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
      return message;
    }

    String token = resolveToken(accessor)
        .orElseThrow(() -> new BaseException(ErrorCode.AUTHENTICATION_REQUIRED));

    if (!tokenProvider.validateAccessToken(token)) {
      throw new BaseException(ErrorCode.INVALID_TOKEN);
    }

    UUID tokenUserId = tokenProvider.getUserId(token);
    UUID sid = tokenProvider.getSid(token);

    String email = tokenProvider.getUsernameFromToken(token);

    UserDetails userDetails;

    try {
      loginSessionStore.findActive(tokenUserId, sid)
          .orElseThrow(() ->
              new BaseException(ErrorCode.LOGIN_SESSION_INVALIDATED));

      userDetails = userDetailsService.loadUserByUsername(email);

    } catch (UsernameNotFoundException e) {
      throw new BaseException(ErrorCode.INVALID_TOKEN, e);

    } catch (DataAccessException e) {
      throw new BaseException(
          ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE,
          e
      );
    }

    if (!tokenUserId.equals(userDetails.getUserDto().id())) {
      throw new BaseException(ErrorCode.INVALID_TOKEN);
    }

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            roleHierarchy.getReachableGrantedAuthorities(
                userDetails.getAuthorities()
            )
        );

    accessor.setUser(authentication);

    log.debug("WebSocket authentication established for user: {}", email);
    return message;
  }

  private Optional<String> resolveToken(StompHeaderAccessor accessor) {
    String prefix = "Bearer ";

    return Optional.ofNullable(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION))
        .filter(value -> value.startsWith(prefix))// 아니면 null
        .map(value -> value.substring(prefix.length()))
        .filter(token -> !token.isBlank());// 토큰이 비어있는지
  }
}
