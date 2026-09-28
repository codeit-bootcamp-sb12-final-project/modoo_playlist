package com.codeit.modoo_playlist.modulerealtime.config;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.modulerealtime.security.RealtimeAuthenticationService;
import com.codeit.modoo_playlist.modulerealtime.security.SseAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSocketSecurityConfig {
    // SecurityConfig에서 WebSocket 서버만 필요한 설정을 옮김.

    @Bean
    public SecurityFilterChain webSocketFilterChain(
            HttpSecurity http,
            RealtimeAuthenticationService authenticationService
    ) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        // SSE의 비동기 후속 디스패치 허용
                        .dispatcherTypeMatchers(
                                DispatcherType.ASYNC,
                                DispatcherType.ERROR
                        ).permitAll()

                        .requestMatchers("/ws","/ws/**","/error").permitAll()

                        // 최초 SSE 요청에 인증 및 USER 권한 요구
                        .requestMatchers(HttpMethod.GET,"/api/sse")
                        .hasRole("USER")

                        .anyRequest().denyAll()
                )
                .addFilterBefore(
                        new SseAuthenticationFilter(authenticationService),
                        UsernamePasswordAuthenticationFilter.class
                )

                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(request -> {
                            CorsConfiguration config = new CorsConfiguration();
                            config.addAllowedOriginPattern("*");
                            config.addAllowedHeader("*");
                            config.addAllowedMethod("GET");
                            config.addAllowedMethod("POST");
                            config.addAllowedMethod("OPTIONS");
                            config.setAllowCredentials(true);
                            return config;
                        }
                ))
                .sessionManagement( session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                response.setStatus(403)
                        )
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(RequestCacheConfigurer::disable);

        return http.build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(UserRole.ADMIN.name())
                .implies(UserRole.USER.name())
                .build();
    }
}
