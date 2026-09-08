package com.codeit.modoo_playlist.core.global.common.dto.file;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachFileResponse {
    private UUID id;
    private String originFileName;
    private String renamedFileName;
    private Long size;
    private String contentType;
    private String url;       // dev: /files/attachments/{파일명} / prod: Presigned URL
    private Instant createdAt;
}
