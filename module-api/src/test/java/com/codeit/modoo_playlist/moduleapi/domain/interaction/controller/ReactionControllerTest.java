package com.codeit.modoo_playlist.moduleapi.domain.interaction.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.dto.request.ReactionRequest;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.service.ReactionService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;

@ExtendWith(MockitoExtension.class)
class ReactionControllerTest {

  @Mock private ReactionService reactionService;
  @InjectMocks private ReactionController controller;

  @Test
  void 로그인_사용자의_반응을_설정하고_202를_반환한다() {
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    UserDetails user = new UserDetails(
        new UserDto(userId, "user@test.com", "user", null, UserRole.USER, false, null), "password");

    ResponseEntity<Void> response =
        controller.setReaction(contentId, new ReactionRequest(InteractionType.LIKE), user);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    verify(reactionService).setReaction(userId, contentId, InteractionType.LIKE);
  }
}
