package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
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
import org.springframework.data.domain.Pageable;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.repository.UserContentInteractionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.SimilarUserDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.repository.UserPreferenceTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.preference.repository.UserSimilarityRepository;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarUserInteractionProjection;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

  @Mock private ContentTagRepository contentTagRepository;
  @Mock private ContentRepository contentRepository;
  @Mock private UserSimilarityRepository userSimilarityRepository;
  @Mock private UserContentInteractionRepository userContentInteractionRepository;
  @Mock private UserPreferenceTagRepository userPreferenceTagRepository;

  private RecommendationServiceImpl service() {
    return new RecommendationServiceImpl(
        contentTagRepository, contentRepository, userSimilarityRepository,
        userContentInteractionRepository, userPreferenceTagRepository);
  }

  @Test
  void getSimilarContents는_존재하지_않는_콘텐츠면_예외를_던진다() {
    UUID contentId = UUID.randomUUID();
    when(contentRepository.existsByIdAndDeletedAtIsNull(contentId)).thenReturn(false);

    assertThatThrownBy(() -> service().getSimilarContents(contentId, 5))
        .isInstanceOf(BaseException.class)
        .satisfies(e -> assertThat(((BaseException) e).getErrorCode()).isEqualTo(ErrorCode.CONTENT_NOT_FOUND));
  }

  @Test
  void getSimilarContents는_후보가_없으면_빈_리스트를_반환한다() {
    UUID contentId = UUID.randomUUID();
    when(contentRepository.existsByIdAndDeletedAtIsNull(contentId)).thenReturn(true);
    when(contentTagRepository.findSimilarContents(eq(contentId), any(Pageable.class))).thenReturn(List.of());

    assertThat(service().getSimilarContents(contentId, 5)).isEmpty();
  }

  @Test
  void getSimilarContents는_태그_벡터_코사인_유사도로_정렬한다() {
    UUID targetId = UUID.randomUUID();
    UUID moreSimilarId = UUID.randomUUID();
    UUID lessSimilarId = UUID.randomUUID();

    Tag tagA = Tag.builder().id(UUID.randomUUID()).name("액션").kind(TagKind.GENRE).contentCount(10).build();
    Tag tagB = Tag.builder().id(UUID.randomUUID()).name("드라마").kind(TagKind.GENRE).contentCount(50).build();

    Content target = content(targetId);
    Content moreSimilar = content(moreSimilarId);
    Content lessSimilar = content(lessSimilarId);

    when(contentRepository.existsByIdAndDeletedAtIsNull(targetId)).thenReturn(true);
    when(contentTagRepository.findSimilarContents(eq(targetId), any(Pageable.class))).thenReturn(List.of(
        new RecommendedContentDto(lessSimilarId, "덜비슷함", null, 0),
        new RecommendedContentDto(moreSimilarId, "더비슷함", null, 0)
    ));
    when(contentRepository.count()).thenReturn(100L);
    when(contentTagRepository.findAllWithTagByContentIds(anyCollection())).thenReturn(List.of(
        contentTag(target, tagA), contentTag(target, tagB),
        contentTag(moreSimilar, tagA), contentTag(moreSimilar, tagB),
        contentTag(lessSimilar, tagB)
    ));

    List<RecommendedContentDto> result = service().getSimilarContents(targetId, 5);

    assertThat(result).extracting(RecommendedContentDto::contentId)
        .containsExactly(moreSimilarId, lessSimilarId);
  }

  @Test
  void getRecommendationsForMe는_유사_사용자가_없으면_빈_리스트를_반환한다() {
    UUID userId = UUID.randomUUID();
    when(userSimilarityRepository.findTopSimilarUsersByUserId(eq(userId), any(PageRequest.class)))
        .thenReturn(List.of());

    assertThat(service().getRecommendationsForMe(userId, 5)).isEmpty();
  }

  @Test
  void getRecommendationsForMe는_후보_콘텐츠가_없으면_빈_리스트를_반환한다() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    when(userSimilarityRepository.findTopSimilarUsersByUserId(eq(userId), any(PageRequest.class)))
        .thenReturn(List.of(new SimilarUserDto(otherUserId, "user2", null, new BigDecimal("0.8"), null)));
    when(userContentInteractionRepository.findCandidateContentIds(eq(userId), anyList(), any(Pageable.class)))
        .thenReturn(List.of());

    assertThat(service().getRecommendationsForMe(userId, 5)).isEmpty();
  }

  @Test
  void getRecommendationsForMe는_유사도로_가중합산하고_0이하는_제외한_뒤_점수순으로_정렬한다() {
    UUID userId = UUID.randomUUID();
    UUID u2 = UUID.randomUUID();
    UUID u3 = UUID.randomUUID();
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();
    UUID c3 = UUID.randomUUID();

    when(userSimilarityRepository.findTopSimilarUsersByUserId(eq(userId), any(PageRequest.class))).thenReturn(List.of(
        new SimilarUserDto(u2, "user2", null, new BigDecimal("0.8"), null),
        new SimilarUserDto(u3, "user3", null, new BigDecimal("0.2"), null)
    ));
    when(userContentInteractionRepository.findCandidateContentIds(eq(userId), anyList(), any(Pageable.class)))
        .thenReturn(List.of(c1, c2, c3));
    when(userContentInteractionRepository.findInteractionsByContentIds(anyList(), anyList()))
        .thenReturn(List.of(
            new SimilarUserInteractionProjection(c1, "c1", null, u2, InteractionType.LIKE, null, 1),
            new SimilarUserInteractionProjection(c2, "c2", null, u3, InteractionType.DISLIKE, null, 1),
            new SimilarUserInteractionProjection(c3, "c3", null, u2, InteractionType.LIKE, null, 1),
            new SimilarUserInteractionProjection(c3, "c3", null, u3, InteractionType.LIKE, null, 1)
        ));

    List<RecommendedContentDto> result = service().getRecommendationsForMe(userId, 5);

    assertThat(result).extracting(RecommendedContentDto::contentId).containsExactly(c3, c1);
  }

  @Test
  void getRecommendationsForMe는_limit개까지만_반환한다() {
    UUID userId = UUID.randomUUID();
    UUID u2 = UUID.randomUUID();
    UUID c1 = UUID.randomUUID();
    UUID c2 = UUID.randomUUID();

    when(userSimilarityRepository.findTopSimilarUsersByUserId(eq(userId), any(PageRequest.class))).thenReturn(
        List.of(new SimilarUserDto(u2, "user2", null, new BigDecimal("1.0"), null)));
    when(userContentInteractionRepository.findCandidateContentIds(eq(userId), anyList(), any(Pageable.class)))
        .thenReturn(List.of(c1, c2));
    when(userContentInteractionRepository.findInteractionsByContentIds(anyList(), anyList()))
        .thenReturn(List.of(
            new SimilarUserInteractionProjection(c1, "c1", null, u2, InteractionType.LIKE, null, 1),
            new SimilarUserInteractionProjection(c2, "c2", null, u2, InteractionType.LIKE, null, 1)
        ));

    assertThat(service().getRecommendationsForMe(userId, 1)).hasSize(1);
  }

  @Test
  void getTrendingContents는_평점_상위_콘텐츠를_DTO로_변환한다() {
    UUID contentId = UUID.randomUUID();
    Content topRated = Content.builder().id(contentId).type(ContentType.MOVIE).title("인기영화")
        .thumbnailUrl("thumb").averageRating(new BigDecimal("4.5")).build();
    when(contentRepository.findTopRated(any(Pageable.class))).thenReturn(List.of(topRated));

    List<RecommendedContentDto> result = service().getTrendingContents(5);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).contentId()).isEqualTo(contentId);
    assertThat(result.get(0).score()).isEqualTo(4.5);
  }

  @Test
  void getTopTagMatchContents는_리포지토리_결과를_그대로_반환한다() {
    UUID tagId = UUID.randomUUID();
    List<RecommendedContentDto> expected = List.of(
        new RecommendedContentDto(UUID.randomUUID(), "제목", null, 0));
    when(contentTagRepository.findContentsByTagId(eq(tagId), eq(PageRequest.of(0, 5)))).thenReturn(expected);

    assertThat(service().getTopTagMatchContents(tagId, 5)).isEqualTo(expected);
  }

  private Content content(UUID id) {
    return Content.builder().id(id).type(ContentType.MOVIE).title("제목-" + id).build();
  }

  private ContentTag contentTag(Content content, Tag tag) {
    return ContentTag.builder()
        .id(new ContentTagId(content.getId(), tag.getId()))
        .content(content).tag(tag).build();
  }
}
