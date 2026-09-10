package com.codeit.modoo_playlist.moduleapi.domain.follow.controller;

import com.codeit.modoo_playlist.core.domain.follow.entity.Follow;
import com.codeit.modoo_playlist.moduleapi.domain.follow.mapper.FollowMapper;
import com.codeit.modoo_playlist.moduleapi.domain.follow.service.FollowService;
import com.codeit.modoo_playlist.moduleapi.dto.follow.request.FollowRequest;
import com.codeit.modoo_playlist.moduleapi.dto.follow.response.FollowResponse;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final FollowMapper followMapper;

    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<FollowResponse> follow(
            @RequestBody @Valid FollowRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        Follow follow = followService.follow(user.getUserDto().id(), request.followeeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(followMapper.toResponse(follow));
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable UUID followId,
            @AuthenticationPrincipal UserDetails user
    ) {
        followService.unfollow(followId, user.getUserDto().id());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/followed-by-me")
    public ResponseEntity<FollowResponse> getFollowStatus(
            @RequestParam UUID followeeId,
            @AuthenticationPrincipal UserDetails user
    ) {
        Follow follow = followService.getFollowStatus(user.getUserDto().id(), followeeId);
        return ResponseEntity.ok(followMapper.toResponse(follow));
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/count")
    public ResponseEntity<Long> countFollowers(@RequestParam UUID followeeId) {
        return ResponseEntity.ok(followService.countFollowers(followeeId));
    }

}