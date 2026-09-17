package com.codeit.modoo_playlist.moduleapi.config;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.conversation.repository.ConversationRepository;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtAuthenticationChannelInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.UUID;
import java.util.regex.Pattern;

@org.springframework.context.annotation.Configuration
@org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
@lombok.RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtAuthenticationChannelInterceptor jwtAuthenticationChannelInterceptor;

    private static final Pattern SUBSCRIPTION = Pattern.compile(
            "^/sub/(contents|conversations)/"
                    + "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{12})/"
                    + "(chat|watch|direct-messages)$"
    );
    private final ConversationRepository conversationRepository;

    private Message<?> checkSubscription(Message<?> message) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class
                );

        if (accessor == null
                || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        var matcher = SUBSCRIPTION.matcher(
                destination == null ? "" : destination
        );

        if (!matcher.matches()) {
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }

        String resource = matcher.group(1);
        String event = matcher.group(3);

        if ("contents".equals(resource)) {
            if (!"chat".equals(event) && !"watch".equals(event)) {
                throw new BaseException(ErrorCode.ACCESS_DENIED);
            }
            return message;
        }

        if (!"direct-messages".equals(event)) {
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }

        if (!(accessor.getUser() instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails user)) {
            throw new BaseException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        UUID conversationId = UUID.fromString(matcher.group(2));

        if (!conversationRepository.existsParticipant(
                conversationId, user.getUserDto().id()
        )) {
            throw new BaseException(ErrorCode.CONVERSATION_ACCESS_DENIED);
        }

        return message;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // server -> client
        config.enableSimpleBroker("/sub")
                .setHeartbeatValue(new long[]{5000, 5000})
                .setTaskScheduler(taskScheduler());
        // client -> server
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.setPreserveReceiveOrder(true);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                jwtAuthenticationChannelInterceptor,
                new SecurityContextChannelInterceptor(),
                authorizationChannelInterceptor(),
                new ChannelInterceptor() {
                    @Override
                    public Message<?> preSend(
                            Message<?> message,
                            MessageChannel channel
                    ) {
                        return checkSubscription(message);
                    }
                }
        );
    }

    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler =
                new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("modoo-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(5);

        return scheduler;
    }

    private AuthorizationChannelInterceptor authorizationChannelInterceptor() {
        return new AuthorizationChannelInterceptor(
                MessageMatcherDelegatingAuthorizationManager.builder()
                        // 연결 시 인증 필요
                        .simpTypeMatchers(SimpMessageType.CONNECT)
                        .hasRole(UserRole.USER.name())

                        // 시청 및 채팅 구독은 인증된 사용자만 허용
                        .simpSubscribeDestMatchers("/sub/**")
                        .hasRole(UserRole.USER.name())

                        // 클라이언트가 서버로 보내는 메시지 인증
                        .simpDestMatchers("/pub/**")
                        .hasRole(UserRole.USER.name())

                        // 연결 정리시 필요
                        .simpTypeMatchers(
                                SimpMessageType.HEARTBEAT,
                                SimpMessageType.UNSUBSCRIBE,
                                SimpMessageType.DISCONNECT
                        )
                        .permitAll()

                        // 기타 등등
                        .anyMessage()
                        .denyAll()
                        .build()
        );
    }
}
