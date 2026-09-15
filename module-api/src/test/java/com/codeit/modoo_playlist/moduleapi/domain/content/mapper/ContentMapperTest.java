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
import com.codeit.modoo_playlist.core.domain.content.entity.ContentSports;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.content.type.SportsStatus;
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
                null, sports, List.of());

        assertThat(response.type()).isEqualTo("sport");
        assertThat(response.video()).isNull();
        assertThat(response.sports().sportType()).isEqualTo("Soccer");
        assertThat(response.sports().kickoffAt()).isEqualTo(kickoffAt);
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
}
