package com.codeit.modoo_playlist.moduleapi.domain.follow.controller;

import com.codeit.modoo_playlist.moduleapi.domain.follow.service.FollowService;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{followeeId}")
    public ResponseEntity<Void> follow(
            @PathVariable UUID followeeId,
            @AuthenticationPrincipal UserDetails user
    ) {
        followService.follow(user.getUserDto().id(), followeeId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{followeeId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable UUID followeeId,
            @AuthenticationPrincipal UserDetails user
    ) {
        followService.unfollow(user.getUserDto().id(), followeeId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{followeeId}/me")
    public ResponseEntity<Boolean> isFollowedByMe(
            @PathVariable UUID followeeId,
            @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity.ok(followService.isFollowedByMe(user.getUserDto().id(), followeeId));
    }

    @GetMapping("/{followeeId}/count")
    public ResponseEntity<Long> countFollowers(@PathVariable UUID followeeId) {
        return ResponseEntity.ok(followService.countFollowers(followeeId));
    }

}