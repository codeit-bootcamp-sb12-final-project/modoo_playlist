package com.codeit.modoo_playlist.moduleapi.support;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.infra.config.QuerydslConfig;
import com.codeit.modoo_playlist.moduleapi.config.OAuth2AuthorizationRequestConfig;
import com.codeit.modoo_playlist.moduleapi.config.SecurityConfig;
import com.codeit.modoo_playlist.moduleapi.config.properties.AuthCookieProperties;
import com.codeit.modoo_playlist.moduleapi.config.properties.CorsProperties;
import com.codeit.modoo_playlist.moduleapi.domain.user.controller.AuthController;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageStorage;
import com.codeit.modoo_playlist.moduleapi.domain.user.controller.UserController;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.TemporaryPasswordGenerator;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.TemporaryPasswordSender;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.AuthServiceImpl;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.OAuthAccountServiceImpl;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.OAuthWithdrawalServiceImpl;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.OAuthWithdrawalTransactionService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.RestSocialAccountUnlinkClient;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.impl.UserServiceImpl;
import com.codeit.modoo_playlist.moduleapi.exception.GlobalExceptionHandler;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import com.codeit.modoo_playlist.moduleapi.security.Http403ForbiddenAccessDeniedHandler;
import com.codeit.modoo_playlist.moduleapi.security.LoginFailureHandler;
import com.codeit.modoo_playlist.moduleapi.security.SecurityErrorResponseWriter;
import com.codeit.modoo_playlist.moduleapi.security.UserAuthenticationProvider;
import com.codeit.modoo_playlist.moduleapi.security.UserDetailsService;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtAuthenticationFilter;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtLoginSuccessHandler;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtLogoutHandler;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import com.codeit.modoo_playlist.infra.store.RedisLoginSessionStore;
import com.codeit.modoo_playlist.moduleapi.security.jwt.RefreshTokenHasher;
import com.codeit.modoo_playlist.moduleapi.security.oauth.GoogleOAuthUserProfileMapper;
import com.codeit.modoo_playlist.moduleapi.security.oauth.KakaoOAuthUserProfileMapper;
import com.codeit.modoo_playlist.moduleapi.security.oauth.OAuthLoginSuccessHandler;
import com.codeit.modoo_playlist.moduleapi.security.oauth.OAuthOidcUserService;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequestStore;
import java.time.Clock;
import org.mapstruct.factory.Mappers;
import org.springframework.ai.model.chat.memory.repository.redis.autoconfigure.RedisChatMemoryRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import static org.mockito.Mockito.mock;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 실제 인증 구성만 로드하여 외부 AI, Kafka 및 운영 DB 설정과 격리한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = RedisChatMemoryRepositoryAutoConfiguration.class)
@EntityScan(basePackageClasses = User.class)
@EnableJpaRepositories(basePackageClasses = UserRepository.class)
@EnableJpaAuditing
@EnableConfigurationProperties({AuthCookieProperties.class, CorsProperties.class})
@Import({
    SecurityConfig.class,
    QuerydslConfig.class,
    UserController.class,
    AuthController.class,
    UserServiceImpl.class,
    AuthServiceImpl.class,
    UserAuthenticationProvider.class,
    UserDetailsService.class,
    GlobalExceptionHandler.class,
    JwtTokenProvider.class,
    JwtAuthenticationFilter.class,
    JwtLoginSuccessHandler.class,
    JwtLogoutHandler.class,
    LoginFailureHandler.class,
    SecurityErrorResponseWriter.class,
    Http403ForbiddenAccessDeniedHandler.class,
    RedisLoginSessionStore.class,
    RefreshTokenHasher.class,
    OAuth2AuthorizationRequestConfig.class,
    OAuthOidcUserService.class,
    OAuthLoginSuccessHandler.class,
    GoogleOAuthUserProfileMapper.class,
    KakaoOAuthUserProfileMapper.class,
    OAuthAccountServiceImpl.class,
    OAuthWithdrawalServiceImpl.class,
    OAuthWithdrawalTransactionService.class,
    RestSocialAccountUnlinkClient.class,
    OAuthWithdrawalRequestStore.class})
public class AuthTestApplication {

  @Bean
  UserMapper userMapper() {
    return Mappers.getMapper(UserMapper.class);
  }

  @Bean
  ImageStorage imageStorage() {
    return mock(ImageStorage.class);
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  TemporaryPasswordGenerator temporaryPasswordGenerator() {
    return () -> "temporary1!!";
  }

  @Bean
  TemporaryPasswordSender temporaryPasswordSender() {
    return (email, temporaryPassword, expiresAt) -> {
    };
  }

  @Bean(destroyMethod = "stop")
  GenericContainer<?> authRedis() {
    GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);
    redis.start();
    return redis;
  }

  @Bean
  LettuceConnectionFactory redisConnectionFactory(GenericContainer<?> authRedis) {
    return new LettuceConnectionFactory(authRedis.getHost(), authRedis.getMappedPort(6379));
  }
}
