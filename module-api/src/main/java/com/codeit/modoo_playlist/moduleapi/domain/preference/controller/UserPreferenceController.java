package com.codeit.modoo_playlist.moduleapi.domain.preference.controller;

import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagQuery;
import com.codeit.modoo_playlist.moduleapi.domain.preference.service.UserPreferenceTagService;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/preferences/tags")
@RequiredArgsConstructor
public class UserPreferenceController {

	private final UserPreferenceTagService userPreferenceTagService;

	@GetMapping("/me")
	public ResponseEntity<List<UserPreferenceTagDto>> getMyPreferenceTags(
			@Valid @ModelAttribute UserPreferenceTagQuery query,
			@AuthenticationPrincipal UserDetails user) {
		UUID userId = UUID.fromString("11111111-0000-0000-0000-000000000002");
		return ResponseEntity.ok(userPreferenceTagService.getMyPreferenceTags(userId, query.limit()));
	}
}
