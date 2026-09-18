package com.codeit.modoo_playlist.moduleapi.domain.preference.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagQuery;
import com.codeit.modoo_playlist.moduleapi.domain.preference.service.UserPreferenceTagService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;

@ExtendWith(MockitoExtension.class)
class UserPreferenceControllerTest {

  @Mock private UserPreferenceTagService userPreferenceTagService;
  @InjectMocks private UserPreferenceController controller;

  @Test
  void 로그인_사용자_ID와_요청_limit으로_취향_태그_목록을_조회한다() {
    UUID userId = UUID.randomUUID();
    UserDetails user = new UserDetails(
        new UserDto(userId, "user@test.com", "user", null, UserRole.USER, false, null), "password");
    List<UserPreferenceTagDto> expected = List.of(
        new UserPreferenceTagDto(UUID.randomUUID(), "액션", TagKind.GENRE, new BigDecimal("1.0")));
    when(userPreferenceTagService.getMyPreferenceTags(userId, 5)).thenReturn(expected);

    ResponseEntity<List<UserPreferenceTagDto>> response =
        controller.getMyPreferenceTags(new UserPreferenceTagQuery(5), user);

    assertThat(response.getBody()).isEqualTo(expected);
  }
}
