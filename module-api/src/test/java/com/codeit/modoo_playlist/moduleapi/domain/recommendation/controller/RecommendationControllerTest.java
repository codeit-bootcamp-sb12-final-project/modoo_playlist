package com.codeit.modoo_playlist.moduleapi.domain.recommendation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.HomeFeedResponse;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendationQuery;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.HomeFeedService;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

  @Mock private RecommendationService recommendationService;
  @Mock private HomeFeedService homeFeedService;
  @InjectMocks private RecommendationController controller;

  @Test
  void getSimilarContents는_서비스_결과를_200으로_반환한다() {
    UUID contentId = UUID.randomUUID();
    List<RecommendedContentDto> expected = List.of(
        new RecommendedContentDto(UUID.randomUUID(), "제목", null, 1.0));
    when(recommendationService.getSimilarContents(contentId, 10)).thenReturn(expected);

    ResponseEntity<List<RecommendedContentDto>> response =
        controller.getSimilarContents(new RecommendationQuery(contentId, 10));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(expected);
  }

  @Test
  void getHomeFeed는_로그인_사용자면_userId를_전달한다() {
    UUID userId = UUID.randomUUID();
    UserDetails user = userDetails(userId);
    HomeFeedResponse expected = new HomeFeedResponse(List.of());
    when(homeFeedService.getHomeFeed(userId)).thenReturn(expected);

    ResponseEntity<HomeFeedResponse> response = controller.getHomeFeed(user);

    assertThat(response.getBody()).isEqualTo(expected);
  }

  @Test
  void getHomeFeed는_비로그인이면_null_userId를_전달한다() {
    HomeFeedResponse expected = new HomeFeedResponse(List.of());
    when(homeFeedService.getHomeFeed(null)).thenReturn(expected);

    ResponseEntity<HomeFeedResponse> response = controller.getHomeFeed(null);

    assertThat(response.getBody()).isEqualTo(expected);
  }

  private UserDetails userDetails(UUID userId) {
    return new UserDetails(
        new UserDto(userId, "user@test.com", "user", null, UserRole.USER, false, null), "password");
  }
}
