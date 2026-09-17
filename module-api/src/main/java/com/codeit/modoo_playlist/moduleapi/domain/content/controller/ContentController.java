package com.codeit.modoo_playlist.moduleapi.domain.content.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codeit.modoo_playlist.core.global.common.util.KeywordNormalizer;
import com.codeit.modoo_playlist.moduleapi.domain.content.service.ContentService;
import com.codeit.modoo_playlist.moduleapi.domain.search.service.ContentSearchService;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentDetailResponse;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final ContentSearchService contentSearchService;

    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<ContentCursorResponse> getContents(
            @Valid @ModelAttribute ContentListRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        // 검색어가 있으면 ES 검색으로 연결
        String keyword = KeywordNormalizer.normalize(request.keywordLike());
        if (!keyword.isEmpty()) {
            return ResponseEntity.ok(contentSearchService.searchPage(request));
        }
        return ResponseEntity.ok(contentService.getContents(request, user.getUserDto().id()));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDetailResponse> getContent(
            @PathVariable UUID contentId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(contentService.getContent(contentId, user.getUserDto().id()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentDetailResponse> createContent(
            @Valid @RequestPart("request") ContentCreateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        ContentDetailResponse response = contentService.createContent(request, thumbnail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(
            value = "/{contentId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ContentDetailResponse> updateContent(
            @PathVariable UUID contentId,
            @Valid @RequestPart("request") ContentUpdateRequest request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        return ResponseEntity.ok(contentService.updateContent(contentId, request, thumbnail));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID contentId) {
        contentService.deleteContent(contentId);
        return ResponseEntity.noContent().build();
    }
}
