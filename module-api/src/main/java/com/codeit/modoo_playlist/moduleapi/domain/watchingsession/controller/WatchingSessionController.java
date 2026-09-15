package com.codeit.modoo_playlist.moduleapi.domain.watchingsession.controller;

import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.service.WatchingSessionService;
import com.codeit.modoo_playlist.moduleapi.dto.WatchingSessionDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.dto.watchingsession.response.CursorResponseWatchingSessionDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WatchingSessionController {

    private final WatchingSessionService watchingSessionService;
    @GetMapping("/users/{watcherId}/watching-sessions")
    public ResponseEntity<WatchingSessionDto> findWatchingSessionsByUser(
            @PathVariable UUID watcherId
    ) {
        WatchingSessionDto response = watchingSessionService.findByUser(watcherId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/contents/{contentId}/watching-sessions")
    public ResponseEntity<CursorResponseWatchingSessionDto> findWatchingSessionsByContent(
            @PathVariable UUID contentId,
            String watcherNameLike,
            @Valid @ModelAttribute SliceCursorRequest request
    ) {
        CursorResponseWatchingSessionDto response =
                watchingSessionService.findByContent(
                        contentId,
                        watcherNameLike,
                        request
                );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
