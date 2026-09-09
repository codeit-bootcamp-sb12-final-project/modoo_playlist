package com.codeit.modoo_playlist.moduleapi.domain.content.service;

import java.util.UUID;

import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentDetailResponse;

public interface ContentService {

    ContentCursorResponse getContents(ContentListRequest request);

    ContentDetailResponse getContent(UUID contentId);
}
