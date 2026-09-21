package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentPerson;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentSports;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.content.type.SportsStatus;
import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentPersonRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentSportsRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;

@ExtendWith(MockitoExtension.class)
class ContentDetailServiceImplTest {

  @Mock private ContentRepository contentRepository;
  @Mock private ContentTagRepository contentTagRepository;
  @Mock private ContentPersonRepository contentPersonRepository;
  @Mock private ContentSportsRepository contentSportsRepository;

  private ContentDetailServiceImpl service() {
    return new ContentDetailServiceImpl(
        contentRepository, contentTagRepository, contentPersonRepository, contentSportsRepository);
  }

  @Test
  void 빈_목록이면_저장소를_조회하지_않는다() {
    assertThat(service().getDetails(List.of())).isEmpty();

    verifyNoInteractions(contentRepository, contentTagRepository, contentPersonRepository, contentSportsRepository);
  }

  @Test
  void 기본정보와_태그_감독_출연진_줄거리를_담는다() {
    UUID id = UUID.randomUUID();
    Content content = content(id, "기생충", "반지하 가족의 이야기");
    List<UUID> ids = List.of(id);
    when(contentRepository.findAllById(ids)).thenReturn(List.of(content));
    when(contentTagRepository.findAllWithTagByContentIds(ids)).thenReturn(List.of(
        contentTag(content, tag("스릴러")), contentTag(content, tag("블랙코미디"))));
    when(contentPersonRepository.findAllByContent_IdOrderByDisplayOrderAsc(id)).thenReturn(List.of(
        person(content, "DIRECTOR", "봉준호", 0),
        person(content, "ACTOR", "송강호", 1),
        person(content, "ACTOR", "이선균", 2)));

    List<ContentDetailDto> result = service().getDetails(ids);

    assertThat(result).singleElement().satisfies(detail -> {
      assertThat(detail.contentId()).isEqualTo(id);
      assertThat(detail.title()).isEqualTo("기생충");
      assertThat(detail.type()).isEqualTo(ContentType.MOVIE);
      assertThat(detail.releaseYear()).isEqualTo(2019);
      assertThat(detail.originCountry()).isEqualTo("KR");
      assertThat(detail.tags()).containsExactly("블랙코미디", "스릴러");
      assertThat(detail.directors()).containsExactly("봉준호");
      assertThat(detail.actors()).containsExactly("송강호", "이선균");
      assertThat(detail.description()).isEqualTo("반지하 가족의 이야기");
      assertThat(detail.sports()).isNull();
    });
  }

  @Test
  void 출연진은_상위_5명만_담는다() {
    UUID id = UUID.randomUUID();
    Content content = content(id, "영화", "설명");
    when(contentRepository.findAllById(List.of(id))).thenReturn(List.of(content));
    when(contentPersonRepository.findAllByContent_IdOrderByDisplayOrderAsc(id)).thenReturn(List.of(
        person(content, "ACTOR", "배우0", 0), person(content, "ACTOR", "배우1", 1),
        person(content, "ACTOR", "배우2", 2), person(content, "ACTOR", "배우3", 3),
        person(content, "ACTOR", "배우4", 4), person(content, "ACTOR", "배우5", 5)));

    List<ContentDetailDto> result = service().getDetails(List.of(id));

    assertThat(result.get(0).actors()).containsExactly("배우0", "배우1", "배우2", "배우3", "배우4");
  }

  @Test
  void 줄거리가_300자를_넘으면_잘라서_말줄임표를_붙인다() {
    UUID id = UUID.randomUUID();
    when(contentRepository.findAllById(List.of(id)))
        .thenReturn(List.of(content(id, "영화", "가".repeat(301))));

    List<ContentDetailDto> result = service().getDetails(List.of(id));

    assertThat(result.get(0).description()).isEqualTo("가".repeat(300) + "…");
  }

  @Test
  void 줄거리가_300자_이하이면_그대로_담고_없으면_null이다() {
    UUID exact = UUID.randomUUID();
    UUID none = UUID.randomUUID();
    when(contentRepository.findAllById(List.of(exact, none))).thenReturn(List.of(
        content(exact, "영화1", "가".repeat(300)), content(none, "영화2", null)));

    List<ContentDetailDto> result = service().getDetails(List.of(exact, none));

    assertThat(result.get(0).description()).isEqualTo("가".repeat(300));
    assertThat(result.get(1).description()).isNull();
  }

  @Test
  void 삭제됐거나_조회되지_않는_콘텐츠는_제외하고_입력_순서를_유지한다() {
    UUID first = UUID.randomUUID();
    UUID deleted = UUID.randomUUID();
    UUID missing = UUID.randomUUID();
    UUID last = UUID.randomUUID();
    Content deletedContent = Content.builder().id(deleted).type(ContentType.MOVIE).title("삭제됨")
        .deletedAt(Instant.now()).build();
    List<UUID> ids = List.of(last, deleted, missing, first);
    when(contentRepository.findAllById(ids)).thenReturn(List.of(
        content(first, "첫째", null), deletedContent, content(last, "마지막", null)));

    List<ContentDetailDto> result = service().getDetails(ids);

    assertThat(result).extracting(ContentDetailDto::contentId).containsExactly(last, first);
  }

  @Test
  void 스포츠_콘텐츠는_스포츠_정보를_담고_일반_콘텐츠는_null이다() {
    UUID sportsId = UUID.randomUUID();
    UUID movieId = UUID.randomUUID();
    Instant kickoff = Instant.parse("2026-09-20T10:00:00Z");
    Content match = Content.builder().id(sportsId).type(ContentType.SPORT).title("맨유 vs 아스날").build();
    List<UUID> ids = List.of(sportsId, movieId);
    when(contentRepository.findAllById(ids)).thenReturn(List.of(match, content(movieId, "영화", null)));
    when(contentSportsRepository.findAllById(ids)).thenReturn(List.of(ContentSports.builder()
        .contentId(sportsId).sportType("Soccer").league("Premier League").season("2025-2026")
        .homeTeam("Man Utd").awayTeam("Arsenal").venue("Old Trafford")
        .status(SportsStatus.SCHEDULED).kickoffAt(kickoff).build()));

    List<ContentDetailDto> result = service().getDetails(ids);

    assertThat(result.get(0).sports()).isEqualTo(new ContentDetailDto.SportsInfo(
        "Soccer", "Premier League", "2025-2026", "Man Utd", "Arsenal", "Old Trafford",
        SportsStatus.SCHEDULED, kickoff));
    assertThat(result.get(1).sports()).isNull();
  }

  private Content content(UUID id, String title, String description) {
    return Content.builder().id(id).type(ContentType.MOVIE).title(title).description(description)
        .releaseDate(LocalDate.of(2019, 5, 30)).originCountry("KR").build();
  }

  private Tag tag(String name) {
    return Tag.builder().id(UUID.randomUUID()).name(name).kind(TagKind.GENRE).build();
  }

  private ContentTag contentTag(Content content, Tag tag) {
    return ContentTag.builder()
        .id(new ContentTagId(content.getId(), tag.getId()))
        .content(content).tag(tag).build();
  }

  private ContentPerson person(Content content, String roleType, String name, int displayOrder) {
    return ContentPerson.builder()
        .content(content).roleType(roleType).personName(name).displayOrder(displayOrder).build();
  }
}
