package com.codeit.modoo_playlist.infra.config.impl;

import com.codeit.modoo_playlist.infra.config.FileConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Profile({"dev", "test"})
public class FileConfigDev implements FileConfig {

    // application.yml의 myblog.storage.local.root-path 값 (기본값: .blog/storage)
    @Value("${myblog.storage.local.root-path:.blog/storage}")
    private String rootPathStr;

    private Path rootPath;

    @PostConstruct
    public void init() throws IOException {
        rootPath = Paths.get(rootPathStr).toAbsolutePath();
        // attachments 하위 폴더까지 미리 생성
        Files.createDirectories(rootPath.resolve("attachments"));
    }

    // 파일 저장 루트 경로 반환
    @Override
    public Path getRootPath() {
        return rootPath;
    }
}
