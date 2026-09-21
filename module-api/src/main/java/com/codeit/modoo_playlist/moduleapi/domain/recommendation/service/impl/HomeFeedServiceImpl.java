package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.impl;

import com.codeit.modoo_playlist.moduleapi.domain.follow.repository.FollowRepository;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.repository.UserContentInteractionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.service.UserPreferenceTagService;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.HomeFeedResponse;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.HomeRowDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.HomeFeedService;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.RecommendationService;
import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository.WatchingSessionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeFeedServiceImpl implements HomeFeedService {

  private static final int ROW_LIMIT = 10;

  private final Random random = new Random();

  private final RecommendationService recommendationService;
  private final UserPreferenceTagService userPreferenceTagService;
  private final WatchingSessionRepository watchingSessionRepository;
  private final FollowRepository followRepository;
  private final UserContentInteractionRepository userContentInteractionRepository;

  @Override
  public HomeFeedResponse getHomeFeed(UUID userId) {
    List<HomeRowDto> rows = new ArrayList<>();
    addIfPresent(rows, "지금 함께 보는 중", this::liveWatchingRow);
    addIfPresent(rows, "취향과 맞아요", () -> topTagMatchRow(userId));
    addIfPresent(rows, "오늘의 추천", () -> todayRecommendationRow(userId));
    addIfPresent(rows, "팔로우한 사람들이 본", () -> followingActivityRow(userId));
    addIfPresent(rows, "인기 콘텐츠", this::trendingRow);
    return new HomeFeedResponse(rows);
  }

  private void addIfPresent(List<HomeRowDto> rows, String rowName, Supplier<HomeRowDto> supplier) {
    HomeRowDto row;
    try {
      row = supplier.get();
    } catch (Exception e) {
      log.error("홈 피드 행 생성 실패: {}", rowName, e);
      return;
    }
    if (row != null && row.contents() != null && !row.contents().isEmpty()) {
      rows.add(row);
    }
  }

  private HomeRowDto liveWatchingRow() {
    List<RecommendedContentDto> contents = watchingSessionRepository
        .findLiveWatchingContents(PageRequest.of(0, ROW_LIMIT));
    if (contents.isEmpty()) {
      return null;
    }
    return new HomeRowDto("지금 함께 보는 중", null, contents);
  }

  private HomeRowDto topTagMatchRow(UUID userId) {
    List<UserPreferenceTagDto> topTags = userPreferenceTagService.getMyPreferenceTags(userId, 5);
    if (topTags.isEmpty()) {
      return null;
    }
    UserPreferenceTagDto picked = pickWeightedRandom(topTags);
    String tagName = picked.tagName();
    return new HomeRowDto(tagName + " 취향과 맞아요", tagName + " 콘텐츠를 모아봤어요",
        recommendationService.getTopTagMatchContents(picked.tagId(), ROW_LIMIT));
  }

  private HomeRowDto todayRecommendationRow(UUID userId) {
    return new HomeRowDto("오늘의 추천", "취향이 비슷한 사용자들이 좋아한 콘텐츠예요",
        recommendationService.getRecommendationsForMe(userId, ROW_LIMIT));
  }

  private HomeRowDto followingActivityRow(UUID userId) {
    List<UUID> followeeIds = followRepository.findFolloweeIdsByFollowerId(userId);
    if (followeeIds.isEmpty()) {
      return null;
    }
    List<RecommendedContentDto> contents = userContentInteractionRepository
        .findMostInteractedContentsByUsers(followeeIds, PageRequest.of(0, ROW_LIMIT));
    if (contents.isEmpty()) {
      return null;
    }

    return new HomeRowDto("팔로우한 사람들이 본", null, contents);
  }

  private HomeRowDto trendingRow() {
    return new HomeRowDto("인기 콘텐츠", null, recommendationService.getTrendingContents(ROW_LIMIT));
  }

  private UserPreferenceTagDto pickWeightedRandom(List<UserPreferenceTagDto> tags){
    double totalWeight = tags.stream().mapToDouble(t -> t.score().doubleValue()).sum();
    double pick = random.nextDouble() * totalWeight;
    double sum = 0;
    for (UserPreferenceTagDto tag : tags) {
      sum += tag.score().doubleValue();
      if (pick < sum) {
        return tag;
      }
    }
    return tags.get(0);
  }
}
