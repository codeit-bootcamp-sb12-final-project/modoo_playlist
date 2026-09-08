package com.codeit.modoo_playlist.moduleapi.config;

import com.codeit.blog.entity.UserRole;
import com.codeit.blog.security.Http403ForbiddenAccessDeniedHandler;
import com.codeit.blog.security.LoginFailureHandler;
import com.codeit.blog.security.SpaCsrfTokenRequestHandler;
import com.codeit.blog.security.jwt.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            Http403ForbiddenAccessDeniedHandler http403ForbiddenAccessDeniedHandler,
            JwtLoginSuccessHandler jwtLoginSuccessHandler,
            JwtLogoutHandler jwtLogoutHandler,
            LoginFailureHandler loginFailureHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        http
                // 1) URL별 인가 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/ws/**",
                                "/error",
                                "/",
                                "/index.html"
                        ).permitAll()

                        .requestMatchers("/api/auth/csrf-token").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/refresh").permitAll()

                        // 그 외 요청은 현재는 개발 편의를 위해 모두 허용
                        .anyRequest().permitAll()
                )

                // 2) CSRF 설정 (Cookie 방식)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/auth/logout")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )

                // 3) form login 사용 (JWT 발급용 로그인 엔드포인트)
                .formLogin(login -> login
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler(jwtLoginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                )

                // 4) JWT 기반 로그아웃
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .addLogoutHandler(jwtLogoutHandler)
                        .logoutSuccessHandler(
                                new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                )

                // 5) 예외 처리
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
                        .accessDeniedHandler(http403ForbiddenAccessDeniedHandler)
                )

                // 6) 세션 정책: 완전 Stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 7) 매 요청마다 Authorization 헤더의 JWT를 검증하는 필터
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        // 8) CORS 설정
        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.addAllowedOriginPattern("*");
            config.addAllowedHeader("*");
            config.addAllowedMethod("*");
            config.setAllowCredentials(true);
            return config;
        }));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(UserRole.ADMIN.name())
                .implies(UserRole.USER.name())
                .build();
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public CommandLineRunner debugFilterChain(SecurityFilterChain filterChain) {
        return args -> {
            int filterSize = filterChain.getFilters().size();
            List<String> filterNames = IntStream.range(0, filterSize)
                    .mapToObj(idx -> String.format("\t[%s/%s] %s",
                            idx + 1,
                            filterSize,
                            filterChain.getFilters().get(idx).getClass()))
                    .toList();
            log.debug("Debug Filter Chain...\n{}", String.join(System.lineSeparator(), filterNames));
        };
    }

    // JWT 세션 저장소: prod는 Redis (서버 재시작/스케일아웃에도 세션 유지)
    @Profile("prod")
    @Bean
    public JwtRegistry<java.util.UUID> jwtRegistry(
            RedisTemplate<String, Object> redisTemplate,
            JwtTokenProvider jwtTokenProvider
    ) {
        return new RedisJwtRegistry(
                redisTemplate,
                2,
                jwtTokenProvider.getAccessTokenExpirationMs(),
                jwtTokenProvider.getRefreshTokenExpirationMs()
        );
    }

    // JWT 세션 저장소: dev/test는 Redis 없이 JVM 메모리에만 저장
    @Profile("!prod")
    @Bean
    public JwtRegistry<java.util.UUID> devJwtRegistry(JwtTokenProvider jwtTokenProvider) {
        return new InMemoryJwtRegistry(2, jwtTokenProvider);
    }
}
