package com.codeit.modoo_playlist.infra.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")           // 모든 경로에 대해
                .allowedOriginPatterns("*")
                .allowedMethods("*")         // 모든 HTTP 메소드 허용
                .allowedHeaders("*")         // 모든 요청 헤더 허용
                .allowCredentials(true)      // 인증정보 포함
                .maxAge(3600);               // Preflight 캐시 1시간
    }

    // 모든 요청에 requestId / method / uri 를 MDC에 주입 (로그 추적용)
    // 정적 리소스 서빙(/files/**)은 FileResourceConfig(dev 전용)에서 담당
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MDCLoggingInterceptor())
                .addPathPatterns("/**");
    }
}
