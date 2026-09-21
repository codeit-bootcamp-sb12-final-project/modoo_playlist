package com.codeit.modoo_playlist.moduleapi.domain.preference.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.SimilarUserDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagQuery;
import com.codeit.modoo_playlist.moduleapi.domain.preference.service.SimilarUserService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;

@ExtendWith(MockitoExtension.class)
class SimilarUserControllerTest {

  @Mock private SimilarUserService similarUserService;
  @InjectMocks private SimilarUserController controller;

  @Test
  void 로그인_사용자_ID와_요청_limit으로_유사_사용자_목록을_조회한다() {
    UUID userId = UUID.randomUUID();
    UserDetails user = new UserDetails(
        new UserDto(userId, "user@test.com", "user", null, UserRole.USER, false, null), "password");
    List<SimilarUserDto> expected = List.of(new SimilarUserDto(UUID.randomUUID(), "u2", null, null, null));
    when(similarUserService.getMySimilarUsers(userId, 20)).thenReturn(expected);

    ResponseEntity<List<SimilarUserDto>> response =
        controller.getMySimilarUsers(new UserPreferenceTagQuery(20), user);

    assertThat(response.getBody()).isEqualTo(expected);
  }
}
