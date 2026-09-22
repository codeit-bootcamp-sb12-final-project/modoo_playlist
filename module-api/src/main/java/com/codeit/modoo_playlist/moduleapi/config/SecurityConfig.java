package com.codeit.modoo_playlist.moduleapi.config;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.security.Http403ForbiddenAccessDeniedHandler;
import com.codeit.modoo_playlist.moduleapi.security.LoginFailureHandler;
import com.codeit.modoo_playlist.moduleapi.security.SecurityErrorResponseWriter;
import com.codeit.modoo_playlist.moduleapi.security.SpaCsrfTokenRequestHandler;
import com.codeit.modoo_playlist.moduleapi.security.UserAuthenticationProvider;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtAuthenticationFilter;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtLoginSuccessHandler;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtLogoutHandler;
import com.codeit.modoo_playlist.moduleapi.security.oauth.OAuthOidcUserService;
import com.codeit.modoo_playlist.moduleapi.security.oauth.OAuthLoginSuccessHandler;
import java.util.List;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      Http403ForbiddenAccessDeniedHandler http403ForbiddenAccessDeniedHandler,
      JwtLoginSuccessHandler jwtLoginSuccessHandler,
      JwtLogoutHandler jwtLogoutHandler,
      LoginFailureHandler loginFailureHandler,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      SecurityErrorResponseWriter securityErrorResponseWriter,
      UserAuthenticationProvider userAuthenticationProvider,
      OAuthOidcUserService oauthOidcUserService,
      OAuthLoginSuccessHandler oauthLoginSuccessHandler,
      OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver
  ) throws Exception {

    http
        .authenticationProvider(userAuthenticationProvider)
        // 1) URL별 인가 설정
        .authorizeHttpRequests(auth -> auth
            // 정적 리소스
            .requestMatchers(
                "/",
                "/index.html",
                "/assets/**",
                "/favicon.svg",
                "/error"
            ).permitAll()

            // WebSocket
            .requestMatchers(
                "/ws/**"
            ).permitAll()

            // 인증 시작 및 복원
            .requestMatchers(
                "/api/auth/sign-in",
                "/api/auth/reset-password",
                "/api/auth/refresh",
                "/api/auth/csrf-token",
                "/oauth2/**",
                "/login/oauth2/**"
            ).permitAll()

            // 회원가입
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

            // 회원 탈퇴 인증 방식 조회
            .requestMatchers(HttpMethod.GET, "/api/users/me/withdrawal-info").authenticated()
            .requestMatchers(HttpMethod.POST, "/api/users/me/withdraw").authenticated()
            .requestMatchers(
                HttpMethod.POST,
                "/api/users/me/withdrawal/oauth2/authorization"
            ).authenticated()

            // 사용자 수정
            .requestMatchers(HttpMethod.PATCH, "/api/users/*").authenticated()

            // 관리자 기능
            .requestMatchers(
                HttpMethod.PATCH,
                "/api/users/*/role",
                "/api/users/*/locked"
            ).authenticated()
            .requestMatchers(HttpMethod.DELETE, "/api/users/*/purge").authenticated()

            // 로그인 필수 — 내 취향/유사 사용자 조회, 콘텐츠 반응
            .requestMatchers(HttpMethod.PUT, "/api/contents/*/reaction").authenticated()
            .requestMatchers("/api/users/preferences/tags/me").authenticated()
            .requestMatchers("/api/users/similar-users/me").authenticated()

            // 그 외 요청은 현재는 개발 편의를 위해 모두 허용
            .anyRequest().permitAll()
        )

        // 2) CSRF 설정 (Cookie 방식)
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            .ignoringRequestMatchers("/ws/**")
        )

        // 3) form login 사용 (JWT 발급용 로그인 엔드포인트)
        .formLogin(login -> login
            .loginProcessingUrl("/api/auth/sign-in")
            .successHandler(jwtLoginSuccessHandler)
            .failureHandler(loginFailureHandler)
        )

        // 4) OIDC OAuth login
        .oauth2Login(oauth -> oauth
            // "/oauth2/authorization/google"
            // "/oauth2/authorization/kakao"
            .authorizationEndpoint(auth -> auth
                .baseUri("/oauth2/authorization")
                .authorizationRequestResolver(oauth2AuthorizationRequestResolver)
            )
            // 공급자별 Claim을 공통 OAuthUserProfile로 변환.
            .userInfoEndpoint(userInfo -> userInfo
                .oidcUserService(oauthOidcUserService)
            )
            .successHandler(oauthLoginSuccessHandler)
            .failureHandler(loginFailureHandler)
        )

        // 5) JWT 기반 로그아웃
        .logout(logout -> logout
            .logoutUrl("/api/auth/sign-out")
            .addLogoutHandler(jwtLogoutHandler)
            .logoutSuccessHandler(
                new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
        )

        // 5) 예외 처리
        // 토큰이 없는 경우 기본적으로 401 반환
        // 서비스 내부에서 사용자 검증으로 403 반환
        // 만약 공통 인증 오류로 넘기면 이 부분도 변경이 필요함
        // ErrorResponse를 써서 Json으로 넘기는 방식으로 생각 중.
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) ->
                securityErrorResponseWriter.write(
                    response,
                    ErrorCode.AUTHENTICATION_REQUIRED
                ))
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
}
