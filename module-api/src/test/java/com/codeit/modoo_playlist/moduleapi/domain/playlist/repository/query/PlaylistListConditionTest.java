package com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query.PlaylistListCondition.SortDirection;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query.PlaylistListCondition.SortType;

class PlaylistListConditionTest {

    @Test
    void 키워드와_커서의_앞뒤_공백을_정규화한다() {
        UUID ownerId = UUID.randomUUID();
        UUID idAfter = UUID.randomUUID();
        PlaylistListCondition condition = new PlaylistListCondition(
                ownerId, null, "  액션  ", "  cursor-value  ", idAfter, 20,
                SortType.CREATED_AT, SortDirection.DESCENDING
        );

        assertThat(condition.keyword()).isEqualTo("액션");
        assertThat(condition.cursor()).isEqualTo("cursor-value");
    }

    @Test
    void 키워드가_공백뿐이면_null로_정규화한다() {
        PlaylistListCondition condition = new PlaylistListCondition(
                null, null, "   ", null, null, 20, SortType.CREATED_AT, SortDirection.ASCENDING
        );

        assertThat(condition.keyword()).isNull();
    }

    @Test
    void limit이_범위를_벗어나면_예외가_발생한다() {
        assertInvalidQuery(() -> condition(null, null, 0), "limit");
        assertInvalidQuery(() -> condition(null, null, 101), "limit");
    }

    @Test
    void cursor와_idAfter는_함께_전달해야_한다() {
        assertInvalidQuery(() -> condition("cursor", null, 20), "cursor");
        assertInvalidQuery(() -> condition(null, UUID.randomUUID(), 20), "cursor");
    }

    @Test
    void 정렬기준이_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new PlaylistListCondition(
                null, null, null, null, null, 20, null, SortDirection.ASCENDING
        )).isInstanceOfSatisfying(BaseException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_QUERY_INVALID));
    }

    @Test
    void 정렬방향이_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new PlaylistListCondition(
                null, null, null, null, null, 20, SortType.CREATED_AT, null
        )).isInstanceOfSatisfying(BaseException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_QUERY_INVALID));
    }

    private PlaylistListCondition condition(String cursor, UUID idAfter, int limit) {
        return new PlaylistListCondition(
                null, null, null, cursor, idAfter, limit, SortType.CREATED_AT, SortDirection.ASCENDING
        );
    }

    private void assertInvalidQuery(Runnable action, String field) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BaseException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PLAYLIST_QUERY_INVALID);
            assertThat(exception.getDetails()).containsEntry("field", field);
        });
    }
}