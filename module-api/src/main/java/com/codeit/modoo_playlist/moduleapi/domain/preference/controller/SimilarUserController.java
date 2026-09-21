package com.codeit.modoo_playlist.moduleapi.domain.preference.controller;

import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.SimilarUserDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagQuery;
import com.codeit.modoo_playlist.moduleapi.domain.preference.service.SimilarUserService;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/similar-users")
@RequiredArgsConstructor
public class SimilarUserController {

  private final SimilarUserService similarUserService;

  @PreAuthorize("hasRole('USER')")
  @GetMapping("/me")
  public ResponseEntity<List<SimilarUserDto>> getMySimilarUsers(
      @Valid @ModelAttribute UserPreferenceTagQuery query,
      @AuthenticationPrincipal UserDetails user){
    return ResponseEntity.ok(similarUserService.getMySimilarUsers(user.getUserDto().id(), query.limit()));
  }

}
