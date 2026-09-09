package com.codeit.modoo_playlist.moduleapi.domain.content.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codeit.modoo_playlist.moduleapi.domain.content.service.ContentService;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentDetailResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<ContentCursorResponse> getContents(
            @Valid @ModelAttribute ContentListRequest request
    ) {
        return ResponseEntity.ok(contentService.getContents(request));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDetailResponse> getContent(
            @PathVariable UUID contentId
    ) {
        return ResponseEntity.ok(contentService.getContent(contentId));
    }
}
