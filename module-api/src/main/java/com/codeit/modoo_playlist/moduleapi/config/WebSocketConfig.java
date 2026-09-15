package com.codeit.modoo_playlist.moduleapi.config;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtAuthenticationChannelInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@org.springframework.context.annotation.Configuration
@org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
@lombok.RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtAuthenticationChannelInterceptor jwtAuthenticationChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // server -> client
        config.enableSimpleBroker("/sub")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(taskScheduler());
        // client -> server
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                jwtAuthenticationChannelInterceptor,
                new SecurityContextChannelInterceptor(),
                authorizationChannelInterceptor()
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
