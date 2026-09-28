package com.codeit.modoo_playlist.moduleapi.domain.content.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentPerson;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentSports;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentVideo;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.content.type.SportsStatus;
import com.codeit.modoo_playlist.core.domain.content.type.VideoReleaseStatus;
import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentQueryPage;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentDetailResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentListItemResponse;

class ContentMapperTest {

    private final ContentMapper mapper = Mappers.getMapper(ContentMapper.class);

    @Test
    void Content와_추가값을_목록응답으로_변환한다() {
        UUID id = UUID.randomUUID();
        Content content = Content.builder()
                .id(id).type(ContentType.TV).title("드라마").description("설명")
                .releaseDate(LocalDate.of(2026, 9, 1)).averageRating(new BigDecimal("4.5"))
                .reviewCount(12).build();

        ContentListItemResponse response = mapper.toListItem(content, List.of("드라마", "범죄"), 8);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.type()).isEqualTo("tvSeries");
        assertThat(response.tags()).containsExactly("드라마", "범죄");
        assertThat(response.averageRating()).isEqualByComparingTo("4.5");
        assertThat(response.watcherCount()).isEqualTo(8);
    }

    @Test
    void 스포츠_상세정보를_중첩응답으로_변환한다() {
        Content content = Content.builder()
                .id(UUID.randomUUID()).type(ContentType.SPORT).title("경기").build();
        Instant kickoffAt = Instant.parse("2026-09-14T12:00:00Z");
        ContentSports sports = ContentSports.builder()
                .contentId(content.getId()).content(content).sportType("Soccer")
                .league("Premier League").homeTeam("Arsenal").awayTeam("Chelsea")
                .status(SportsStatus.SCHEDULED).kickoffAt(kickoffAt).build();

        ContentDetailResponse response = mapper.toDetail(content, List.of("Soccer"), 3,
                null, sports, List.of(), InteractionType.LIKE);

        assertThat(response.type()).isEqualTo("sport");
        assertThat(response.video()).isNull();
        assertThat(response.sports().sportType()).isEqualTo("Soccer");
        assertThat(response.sports().kickoffAt()).isEqualTo(kickoffAt);
        assertThat(response.myReaction()).isEqualTo(InteractionType.LIKE);
    }

    @Test
    void 페이지_메타정보와_목록을_커서응답으로_변환한다() {
        UUID nextId = UUID.randomUUID();
        ContentQueryPage page = new ContentQueryPage(List.of(), "cursor", nextId, true, 31);

        ContentCursorResponse response = mapper.toCursorResponse(
                page, List.of(), "createdAt", "DESCENDING"
        );

        assertThat(response.nextCursor()).isEqualTo("cursor");
        assertThat(response.nextIdAfter()).isEqualTo(nextId);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.totalCount()).isEqualTo(31);
    }

    @Test
    void 비디오와_등장인물을_상세응답으로_변환한다() {
        Content content = Content.builder()
                .id(UUID.randomUUID()).type(ContentType.MOVIE).title("영화")
                .originCountry("KR").build();
        ContentVideo video = ContentVideo.builder()
                .content(content).runtimeMinutes(121).collectionName("컬렉션")
                .imdbId("tt123").releaseStatus(VideoReleaseStatus.RELEASED)
                .originalLanguage("ko").externalRating(new BigDecimal("8.1"))
                .externalRatingCount(42).build();
        ContentPerson person = ContentPerson.builder()
                .id(UUID.randomUUID()).content(content).roleType("ACTOR")
                .personName("배우").characterName("주인공").displayOrder(0)
                .personId("person-1").personImg("https://image/1.jpg").build();

        ContentDetailResponse response = mapper.toDetail(
                content, List.of("액션"), 9, video, null, List.of(person), null
        );

        assertThat(response.video().runtimeMinutes()).isEqualTo(121);
        assertThat(response.video().releaseStatus()).isEqualTo(VideoReleaseStatus.RELEASED);
        assertThat(response.people()).singleElement().satisfies(mapped -> {
            assertThat(mapped.id()).isEqualTo(person.getId());
            assertThat(mapped.personName()).isEqualTo("배우");
            assertThat(mapped.personImg()).isEqualTo("https://image/1.jpg");
        });
        assertThat(response.sports()).isNull();
    }

    @Test
    void null_입력은_null_응답으로_변환한다() {
        assertThat(mapper.toListItem(null, null, 0)).isNull();
        assertThat(mapper.toDetail(null, null, 0, null, null, null, null)).isNull();
        assertThat(mapper.toCursorResponse(null, null, null, null)).isNull();
        assertThat(mapper.toVideoResponse(null)).isNull();
        assertThat(mapper.toSportsResponse(null)).isNull();
        assertThat(mapper.toPersonResponse(null)).isNull();
    }

    @Test
    void 일부_값만_있는_입력도_안전하게_변환한다() {
        ContentListItemResponse listItem = mapper.toListItem(null, List.of("태그"), 2);
        Content content = Content.builder()
                .id(UUID.randomUUID()).type(ContentType.MOVIE).title("태그 없는 영화").build();
        ContentListItemResponse itemWithoutTags = mapper.toListItem(content, null, 0);
        ContentCursorResponse cursor = mapper.toCursorResponse(
                null, List.of(listItem), "createdAt", "ASCENDING"
        );
        ContentDetailResponse detail = mapper.toDetail(
                null, List.of(), 1, null, null, List.of(), InteractionType.DISLIKE
        );

        assertThat(listItem.id()).isNull();
        assertThat(listItem.tags()).containsExactly("태그");
        assertThat(itemWithoutTags.id()).isEqualTo(content.getId());
        assertThat(itemWithoutTags.tags()).isNull();
        assertThat(cursor.data()).containsExactly(listItem);
        assertThat(cursor.nextCursor()).isNull();
        assertThat(detail.people()).isEmpty();
        assertThat(detail.myReaction()).isEqualTo(InteractionType.DISLIKE);
    }
}
