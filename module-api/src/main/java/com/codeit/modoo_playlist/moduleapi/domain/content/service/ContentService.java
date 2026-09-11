package com.codeit.modoo_playlist.moduleapi.domain.content.service;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentDetailResponse;

public interface ContentService {

    ContentCursorResponse getContents(ContentListRequest request);

    ContentDetailResponse getContent(UUID contentId);

    ContentDetailResponse createContent(
            ContentCreateRequest request,
            MultipartFile thumbnail
    );

    ContentDetailResponse updateContent(
            UUID contentId,
            ContentUpdateRequest request,
            MultipartFile thumbnail
    );

    void deleteContent(UUID contentId);
}
