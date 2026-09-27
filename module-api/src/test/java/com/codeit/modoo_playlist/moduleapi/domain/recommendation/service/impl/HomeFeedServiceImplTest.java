package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.FollowRepository;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.repository.UserContentInteractionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.service.UserPreferenceTagService;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.HomeFeedResponse;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.HomeRowDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository.ApiWatchingSessionRepository;

@ExtendWith(MockitoExtension.class)
class HomeFeedServiceImplTest {

  @Mock private RecommendationService recommendationService;
  @Mock private UserPreferenceTagService userPreferenceTagService;
  @Mock private ApiWatchingSessionRepository watchingSessionRepository;
  @Mock private FollowRepository followRepository;
  @Mock private UserContentInteractionRepository userContentInteractionRepository;

  private HomeFeedServiceImpl service() {
    return new HomeFeedServiceImpl(
        recommendationService, userPreferenceTagService, watchingSessionRepository,
        followRepository, userContentInteractionRepository);
  }

  @Test
  void 로그인_사용자는_모든_행을_지정된_순서로_구성한다() {
    UUID userId = UUID.randomUUID();
    UUID tagId = UUID.randomUUID();
    when(watchingSessionRepository.findLiveWatchingContents(eq(PageRequest.of(0, 10))))
        .thenReturn(List.of(content("함께보는 콘텐츠")));
    when(userPreferenceTagService.getMyPreferenceTags(userId, 5)).thenReturn(
        List.of(new UserPreferenceTagDto(tagId, "액션", TagKind.GENRE, new BigDecimal("1.0"))));
    when(recommendationService.getTopTagMatchContents(tagId, 10)).thenReturn(List.of(content("액션 콘텐츠")));
    when(recommendationService.getRecommendationsForMe(userId, 10)).thenReturn(List.of(content("추천 콘텐츠")));
    when(followRepository.findFolloweeIdsByFollowerId(userId)).thenReturn(List.of(UUID.randomUUID()));
    when(userContentInteractionRepository.findMostInteractedContentsByUsers(any(), eq(PageRequest.of(0, 10))))
        .thenReturn(List.of(content("팔로우 콘텐츠")));
    when(recommendationService.getTrendingContents(10)).thenReturn(List.of(content("인기 콘텐츠")));

    HomeFeedResponse response = service().getHomeFeed(userId);

    assertThat(response.rows()).extracting(HomeRowDto::title).containsExactly(
        "지금 함께 보는 중", "액션 취향과 맞아요", "오늘의 추천", "팔로우한 사람들이 본", "인기 콘텐츠");
  }

  @Test
  void 콘텐츠가_비어있는_행은_결과에서_빠진다() {
    UUID userId = UUID.randomUUID();
    when(watchingSessionRepository.findLiveWatchingContents(eq(PageRequest.of(0, 10)))).thenReturn(List.of());
    when(userPreferenceTagService.getMyPreferenceTags(userId, 5)).thenReturn(List.of());
    when(recommendationService.getRecommendationsForMe(userId, 10)).thenReturn(List.of(content("추천")));
    when(followRepository.findFolloweeIdsByFollowerId(userId)).thenReturn(List.of());
    when(recommendationService.getTrendingContents(10)).thenReturn(List.of());

    HomeFeedResponse response = service().getHomeFeed(userId);

    assertThat(response.rows()).extracting(HomeRowDto::title).containsExactly("오늘의 추천");
  }

  @Test
  void 한_행에서_예외가_나도_나머지_행은_정상적으로_구성된다() {
    UUID userId = UUID.randomUUID();
    when(watchingSessionRepository.findLiveWatchingContents(eq(PageRequest.of(0, 10)))).thenReturn(List.of());
    when(userPreferenceTagService.getMyPreferenceTags(userId, 5)).thenReturn(List.of());
    when(recommendationService.getRecommendationsForMe(userId, 10))
        .thenThrow(new RuntimeException("추천 서비스 장애"));
    when(followRepository.findFolloweeIdsByFollowerId(userId)).thenReturn(List.of());
    when(recommendationService.getTrendingContents(10)).thenReturn(List.of(content("인기 콘텐츠")));

    HomeFeedResponse response = service().getHomeFeed(userId);

    assertThat(response.rows()).extracting(HomeRowDto::title).containsExactly("인기 콘텐츠");
  }

  private RecommendedContentDto content(String title) {
    return new RecommendedContentDto(UUID.randomUUID(), title, null, 1.0);
  }
}
