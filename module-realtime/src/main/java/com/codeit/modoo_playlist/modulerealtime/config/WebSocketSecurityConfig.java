package com.codeit.modoo_playlist.modulerealtime.config;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
public class WebSocketSecurityConfig {
    // SecurityConfig에서 WebSocket 서버만 필요한 설정을 옮김.

    @Bean
    public SecurityFilterChain webSocketFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws","/ws/**").permitAll()
                        .anyRequest().denyAll()
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**")
                )
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
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

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
