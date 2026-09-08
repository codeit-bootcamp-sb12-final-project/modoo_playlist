package com.codeit.modoo_playlist.infra.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@Profile("dev") // prod(S3)에서는 파일이 로컬에 없으므로 정적 리소스 서빙 불필요
public class FileResourceConfig implements WebMvcConfigurer {

    private final FileConfig fileConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // getRootPath() 기준으로 /files/** URL 노출 (dev 로컬 파일 직접 접근용)
        // FileStorageDev.getAttachFileUrl()의 /files/attachments/{파일명} 경로와 매핑
        String rootLocation = "file:///" + fileConfig.getRootPath().toString().replace("\\", "/") + "/";
        registry.addResourceHandler("/files/**")
                .addResourceLocations(rootLocation);
    }
}
