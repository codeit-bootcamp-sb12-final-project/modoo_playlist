package com.codeit.modoo_playlist.moduleapi.interaction.controller;

import com.codeit.modoo_playlist.moduleapi.interaction.dto.request.ReactionRequest;
import com.codeit.modoo_playlist.moduleapi.interaction.service.ReactionService;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ReactionController {

  private final ReactionService reactionService;

  @PutMapping("/{contentId}/reaction")
  public ResponseEntity<Void> setReaction(@PathVariable UUID contentId,
      @RequestBody @Valid ReactionRequest request,
      @AuthenticationPrincipal UserDetails user) {
    reactionService.setReaction(user.getUserDto().id(), contentId, request.type());
    return ResponseEntity.accepted().build();
  }
}
