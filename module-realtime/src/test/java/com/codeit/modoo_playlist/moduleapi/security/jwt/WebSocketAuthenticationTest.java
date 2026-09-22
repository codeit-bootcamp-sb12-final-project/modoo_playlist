package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.core.global.security.AccessTokenClaims;
import com.codeit.modoo_playlist.core.global.security.LoginSession;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import com.codeit.modoo_playlist.infra.repository.RealtimeConversationRepository;
import com.codeit.modoo_playlist.infra.repository.RealtimeUserRepository;
import com.codeit.modoo_playlist.infra.security.AccessTokenVerifier;
import com.codeit.modoo_playlist.modulerealtime.config.WebSocketConfig;
import com.codeit.modoo_playlist.modulerealtime.config.WebSocketSecurityConfig;
import com.codeit.modoo_playlist.modulerealtime.handler.WebSocketStompErrorHandler;
import com.codeit.modoo_playlist.modulerealtime.security.JwtAuthenticationChannelInterceptor;
import com.codeit.modoo_playlist.modulerealtime.security.RealtimeAuthenticationService;
import com.codeit.modoo_playlist.modulerealtime.security.SubscriptionAuthorizationInterceptor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WebSocketAuthenticationTest {
    private final AccessTokenVerifier verifier = mock(AccessTokenVerifier.class);
    private final LoginSessionStore sessions = mock(LoginSessionStore.class);
    private final RealtimeUserRepository users = mock(RealtimeUserRepository.class);
    private final RealtimeConversationRepository conversations = mock(RealtimeConversationRepository.class);
    private final RealtimeAuthenticationService authentication = new RealtimeAuthenticationService(
            verifier, sessions, users, new WebSocketSecurityConfig().roleHierarchy());
    private final WebSocketConfig config = new WebSocketConfig(
            new JwtAuthenticationChannelInterceptor(authentication),
            new SubscriptionAuthorizationInterceptor(conversations),
            mock(WebSocketStompErrorHandler.class));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void connectWithoutTokenIsRejected() {
        assertThatThrownBy(() -> inbound(frame(StompCommand.CONNECT, null, null)))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED));
        verifyNoInteractions(verifier, sessions, users);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"USER", "ADMIN"})
    void userAndAdminCanConnect(UserRole role) {
        UUID userId = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        User user = mock(User.class);
        when(verifier.verify("valid-token")).thenReturn(new AccessTokenClaims(userId, sid, "user@example.com"));
        when(sessions.findActive(userId, sid)).thenReturn(Optional.of(mock(LoginSession.class)));
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getRole()).thenReturn(role);

        Message<byte[]> message = frame(StompCommand.CONNECT, null, "Bearer valid-token");
        assertThat(inbound(message)).isSameAs(message);
        Authentication principal = (Authentication) StompHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class).getUser();
        assertThat(principal.isAuthenticated()).isTrue();
        assertThat(principal.getAuthorities()).extracting("authority").contains("ROLE_USER");
        verify(sessions).findActive(userId, sid);
        verify(users).findByEmail("user@example.com");
    }

    @Test
    void invalidTokenIsRejected() {
        when(verifier.verify("invalid")).thenThrow(new BaseException(ErrorCode.INVALID_TOKEN));
        assertThatThrownBy(() -> inbound(frame(StompCommand.CONNECT, null, "Bearer invalid")))
                .isInstanceOfSatisfying(BaseException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
        verifyNoInteractions(sessions, users);
    }

    @Test
    void unauthenticatedSubscriptionIsRejected() {
        assertThatThrownBy(() -> inbound(frame(StompCommand.SUBSCRIBE,
                "/sub/contents/" + UUID.randomUUID() + "/chat", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Message<?> inbound(Message<?> message) {
        ChannelRegistration registration = new ChannelRegistration();
        config.configureClientInboundChannel(registration);
        List<ChannelInterceptor> interceptors = ReflectionTestUtils.invokeMethod(registration, "getInterceptors");
        try {
            for (ChannelInterceptor interceptor : interceptors) {
                message = interceptor.preSend(message, null);
            }
            return message;
        } finally {
            for (int i = interceptors.size() - 1; i >= 0; i--) {
                interceptors.get(i).afterSendCompletion(message, null, true, null);
            }
        }
    }

    @Test
    void authenticatedUserCanSubscribeToOwnErrorsOnly() {
        Message<byte[]> message = authenticatedFrame(StompCommand.SUBSCRIBE, "/user/sub/errors");
        assertThat(inbound(message)).isSameAs(message);
        verifyNoInteractions(conversations);

        for (String destination : List.of("/sub/errors", "/sub/errors-userother",
                "/user/other/sub/errors", "/user/sub/errors/other")) {
            assertThatThrownBy(() -> inbound(authenticatedFrame(StompCommand.SUBSCRIBE, destination)))
                    .isInstanceOfAny(BaseException.class, AccessDeniedException.class);
        }
        assertThatThrownBy(() -> inbound(authenticatedFrame(StompCommand.SEND, "/user/sub/errors")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void anonymousUserCannotSubscribeToErrors() {
        assertThatThrownBy(() -> inbound(frame(StompCommand.SUBSCRIBE, "/user/sub/errors", null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Message<byte[]> authenticatedFrame(StompCommand command, String destination) {
        Message<byte[]> message = frame(command, destination, null);
        StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class).setUser(
                UsernamePasswordAuthenticationToken.authenticated(
                        "user@example.com", null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        return message;
    }

    private Message<byte[]> frame(StompCommand command, String destination, String authorization) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(command);
        if (destination != null) headers.setDestination(destination);
        if (authorization != null) headers.setNativeHeader("Authorization", authorization);
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
