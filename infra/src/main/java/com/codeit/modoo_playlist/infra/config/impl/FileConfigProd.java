package com.codeit.modoo_playlist.infra.config.impl;

import com.codeit.modoo_playlist.infra.config.FileConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@Profile("prod")
public class FileConfigProd implements FileConfig {

    // prod는 S3 사용이므로 로컬 경로는 임시 디렉토리 용도 (필요 시 활용)
    @Value("${myblog.storage.local.root-path:/app/storage}")
    private String rootPathStr;

    private Path rootPath;

    @PostConstruct
    public void init() throws IOException {
        rootPath = Path.of(rootPathStr);
        Files.createDirectories(rootPath.resolve("attachments"));
    }

    // 파일 저장 루트 경로 반환
    @Override
    public Path getRootPath() {
        return rootPath;
    }
}
