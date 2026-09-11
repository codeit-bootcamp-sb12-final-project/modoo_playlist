package com.codeit.modoo_playlist.moduleapi.domain.playlist.controller;

import com.codeit.modoo_playlist.moduleapi.domain.playlist.service.PlaylistService;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.request.PlaylistCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.request.PlaylistListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.request.PlaylistUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.response.PlaylistCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.response.PlaylistResponse;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    @GetMapping
    public ResponseEntity<PlaylistCursorResponse> getPlaylists(
            @Valid @ModelAttribute PlaylistListRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID viewerId = (user != null) ? user.getUserDto().id() : null;
        return ResponseEntity.ok(playlistService.getPlaylists(request, viewerId));
    }

    @PostMapping
    public ResponseEntity<PlaylistResponse> createPlaylist(
            @Valid @RequestBody PlaylistCreateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID playlistId = playlistService.createPlaylist(user.getUserDto().id(), request.title(), request.description());
        PlaylistResponse response = playlistService.getPlaylistResponse(playlistId, user.getUserDto().id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistResponse> getPlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID viewerId = (user != null) ? user.getUserDto().id() : null;
        PlaylistResponse response = playlistService.getPlaylistResponse(playlistId, viewerId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{playlistId}")
    public ResponseEntity<PlaylistResponse> updatePlaylist(
            @PathVariable UUID playlistId,
            @Valid @RequestBody PlaylistUpdateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        PlaylistResponse response = playlistService.updatePlaylist(
                playlistId, user.getUserDto().id(), request.title(), request.description());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> deletePlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal UserDetails user
    ) {
        playlistService.deletePlaylist(playlistId, user.getUserDto().id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> addContent(
            @PathVariable UUID playlistId,
            @PathVariable UUID contentId,
            @AuthenticationPrincipal UserDetails user
    ) {
        playlistService.addContent(playlistId, user.getUserDto().id(), contentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> removeContent(
            @PathVariable UUID playlistId,
            @PathVariable UUID contentId,
            @AuthenticationPrincipal UserDetails user
    ) {
        playlistService.removeContent(playlistId, user.getUserDto().id(), contentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> subscribe(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal UserDetails user
    ) {
        playlistService.subscribe(playlistId, user.getUserDto().id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal UserDetails user
    ) {
        playlistService.unsubscribe(playlistId, user.getUserDto().id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{playlistId}/subscriptions/me")
    public ResponseEntity<Boolean> isSubscribedByMe(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(playlistService.isSubscribedByMe(playlistId, user.getUserDto().id()));
    }

    @GetMapping("/{playlistId}/subscriptions/count")
    public ResponseEntity<Long> countSubscribers(@PathVariable UUID playlistId) {
        return ResponseEntity.ok(playlistService.countSubscribers(playlistId));
    }

}