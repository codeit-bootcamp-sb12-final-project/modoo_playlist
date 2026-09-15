package com.codeit.modoo_playlist.modulebatch.sports.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEvent;
import com.codeit.modoo_playlist.modulebatch.sports.config.SportsBatchProperties;
import com.codeit.modoo_playlist.modulebatch.sports.model.ExistingSportsContent;

class SportsSyncConverterTest {

    private final SportsSyncConverter converter = new SportsSyncConverter(new SportsBatchProperties());

    @Test
    void 신규경기를_콘텐츠와_표시용태그로_변환한다() {
        var result = converter.convert(event("NS", null, "2026-12-01T12:00:00Z"), null);

        assertThat(result.sourceId()).isEqualTo("event-1");
        assertThat(result.title()).isEqualTo("Arsenal vs Chelsea");
        assertThat(result.sports().status()).isEqualTo("SCHEDULED");
        assertThat(result.tags()).extracting(tag -> tag.name())
                .containsExactly("축구", "Premier League", "Arsenal", "Chelsea");
    }

    @Test
    void 외부상태를_CANCELED_POSTPONED_FINISHED_LIVE로_정규화한다() {
        assertThat(converter.convert(event("Cancelled", null, "2026-12-01T12:00:00Z"), null)
                .sports().status()).isEqualTo("CANCELED");
        assertThat(converter.convert(event(null, "yes", "2026-12-01T12:00:00Z"), null)
                .sports().status()).isEqualTo("POSTPONED");
        assertThat(converter.convert(event("FT", null, "2026-12-01T12:00:00Z"), null)
                .sports().status()).isEqualTo("FINISHED");
        assertThat(converter.convert(event("LIVE", null, "2026-12-01T12:00:00Z"), null)
                .sports().status()).isEqualTo("LIVE");
    }

    @Test
    void 공백응답은_기존_CANCELED와_POSTPONED_상태를_보존한다() {
        assertThat(converter.convert(event(null, null, null), existing("CANCELED"))
                .sports().status()).isEqualTo("CANCELED");
        assertThat(converter.convert(event(null, null, null), existing("POSTPONED"))
                .sports().status()).isEqualTo("POSTPONED");
    }

    @Test
    void 신규경기의_필수값이나_시각이_없으면_저장대상에서_제외한다() {
        assertInvalid(() -> converter.convert(new SportsDbEvent(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null
        ), null), "idEvent");
        assertInvalid(() -> converter.convert(event("NS", null, null), null), "strTimestamp");
    }

    @Test
    void 썸네일은_이벤트_리그_홈팀_기존값_순서로_보완한다() {
        ExistingSportsContent existing = existing("SCHEDULED");

        assertThat(converter.convert(thumbnailEvent("event.png", "league.png", "home.png"), existing)
                .thumbnailUrl()).isEqualTo("event.png");
        assertThat(converter.convert(thumbnailEvent(null, "league.png", "home.png"), existing)
                .thumbnailUrl()).isEqualTo("league.png");
        assertThat(converter.convert(thumbnailEvent(null, null, "home.png"), existing)
                .thumbnailUrl()).isEqualTo("home.png");
        assertThat(converter.convert(thumbnailEvent(null, null, null), existing)
                .thumbnailUrl()).isEqualTo("old.png");
    }

    private SportsDbEvent event(String status, String postponed, String timestamp) {
        return new SportsDbEvent("event-1", null, "Premier League", "2026", "Soccer",
                "Arsenal", "Chelsea", "Stadium", timestamp, "England",
                "thumb.png", "league.png", "home.png", status, postponed);
    }

    private SportsDbEvent thumbnailEvent(String eventImage, String leagueImage, String homeImage) {
        return new SportsDbEvent("event-1", null, "Premier League", "2026", "Soccer",
                "Arsenal", "Chelsea", null, "2026-12-01T12:00:00Z", "England",
                eventImage, leagueImage, homeImage, "NS", null);
    }

    private ExistingSportsContent existing(String status) {
        return new ExistingSportsContent("id", "old", null, "old.png", null, null, "England",
                "Soccer", "Premier League", "2026", "Arsenal", "Chelsea", "Stadium",
                status, Instant.parse("2026-12-01T12:00:00Z"));
    }

    private void assertInvalid(Runnable action, String field) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BaseException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SPORTSDB_EVENT_INVALID);
            assertThat(exception.getDetails()).containsEntry("field", field);
        });
    }
}
