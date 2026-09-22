package com.codeit.modoo_playlist.modulerealtime.config;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.modulerealtime.handler.WebSocketStompErrorHandler;
import com.codeit.modoo_playlist.modulerealtime.security.JwtAuthenticationChannelInterceptor;
import com.codeit.modoo_playlist.modulerealtime.security.SubscriptionAuthorizationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtAuthenticationChannelInterceptor jwtAuthenticationChannelInterceptor;
    private final SubscriptionAuthorizationInterceptor subscriptionAuthorizationInterceptor;
    private final WebSocketStompErrorHandler webSocketStompErrorHandler;

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
        registry.setErrorHandler(webSocketStompErrorHandler);
        registry.setPreserveReceiveOrder(true);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
                jwtAuthenticationChannelInterceptor, //connect 시 jwt를 확인하고 인증 정보를 설정
                new SecurityContextChannelInterceptor(),
                authorizationChannelInterceptor(), // 로그인 역할에 따른 기본 접근 권한 확인
                subscriptionAuthorizationInterceptor // subscribe시 경로 형식과 대화 참여 여부 확인
        );
    }

    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

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
                        .simpSubscribeDestMatchers("/user/sub/errors", "/sub/**")
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
