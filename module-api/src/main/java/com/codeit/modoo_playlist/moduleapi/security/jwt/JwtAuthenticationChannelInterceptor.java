package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.infra.security.BlogUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final RoleHierarchy roleHierarchy;
    private final BlogUserDetailsService userDetailsService;
    private final JwtRegistry<UUID> jwtRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message,
                StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor)
                    .orElseThrow(() -> new RuntimeException("JWT token is missing"));

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

                accessor.setUser(authentication);
                log.debug("Set authentication for user: {}", username);
            } else {
                log.debug("Invalid JWT token");
                throw new RuntimeException("JWT token is missing");
            }
        }
        return message;
    }

    private Optional<String> resolveToken(StompHeaderAccessor accessor) {
        String prefix = "Bearer ";
        return Optional.ofNullable(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION))
                .map(value -> {
                    if (value.startsWith(prefix)) {
                        return value.substring(prefix.length());
                    } else {
                        return null;
                    }
                });
    }
}
