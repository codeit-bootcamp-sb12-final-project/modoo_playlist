package com.codeit.modoo_playlist.moduleapi.domain.playlist.controller;

import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.service.PlaylistService;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.PlaylistDto;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.request.PlaylistContentAddRequest;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.request.PlaylistCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.request.PlaylistUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.mapper.PlaylistMapper;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final PlaylistMapper playlistMapper;

    @PostMapping
    public ResponseEntity<PlaylistDto> createPlaylist(
            @Valid @RequestBody PlaylistCreateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        UUID playlistId = playlistService.createPlaylist(user.getUserDto().id(), request.title(), request.description());
        Playlist playlist = playlistService.getPlaylist(playlistId);
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistMapper.toDto(playlist));
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> getPlaylist(@PathVariable UUID playlistId) {
        Playlist playlist = playlistService.getPlaylist(playlistId);
        return ResponseEntity.ok(playlistMapper.toDto(playlist));
    }

    @PutMapping("/{playlistId}")
    public ResponseEntity<Void> updatePlaylist(
            @PathVariable UUID playlistId,
            @Valid @RequestBody PlaylistUpdateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        playlistService.updatePlaylist(playlistId, user.getUserDto().id(), request.title(), request.description());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> deletePlaylist(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal UserDetails user
    ) {
        playlistService.deletePlaylist(playlistId, user.getUserDto().id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{playlistId}/contents")
    public ResponseEntity<Void> addContent(
            @PathVariable UUID playlistId,
            @Valid @RequestBody PlaylistContentAddRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        playlistService.addContent(playlistId, user.getUserDto().id(), request.contentId());
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

    @PostMapping("/{playlistId}/subscriptions")
    public ResponseEntity<Void> subscribe(
            @PathVariable UUID playlistId,
            @AuthenticationPrincipal UserDetails user
    ) {
        playlistService.subscribe(playlistId, user.getUserDto().id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{playlistId}/subscriptions")
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