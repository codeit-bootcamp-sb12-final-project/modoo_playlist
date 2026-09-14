package com.codeit.modoo_playlist.moduleapi.domain.content.repository.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentListCondition.SortDirection;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentListCondition.SortType;

class ContentListConditionTest {

    @Test
    void 문자열_조회조건을_정규화한다() {
        UUID idAfter = UUID.randomUUID();
        ContentListCondition condition = new ContentListCondition(
                ContentType.MOVIE, "  액션  ", List.of(" 드라마 ", "액션", "드라마", "  "),
                " 10.0 ", idAfter, 20, SortType.AVERAGE_RATING, SortDirection.DESCENDING
        );

        assertThat(condition.keyword()).isEqualTo("액션");
        assertThat(condition.tagNames()).containsExactly("드라마", "액션");
        assertThat(condition.cursor()).isEqualTo("10.0");
    }

    @Test
    void 태그가_null이면_빈_목록으로_변환한다() {
        assertThat(condition(null, null, 20).tagNames()).isEmpty();
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
    void 정렬조건이_없으면_예외가_발생한다() {
        assertThatThrownBy(() -> new ContentListCondition(
                null, null, null, null, null, 20, null, SortDirection.ASCENDING
        )).isInstanceOfSatisfying(BaseException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_QUERY_INVALID));
    }

    private ContentListCondition condition(String cursor, UUID idAfter, int limit) {
        return new ContentListCondition(
                null, null, null, cursor, idAfter, limit, SortType.CREATED_AT, SortDirection.ASCENDING
        );
    }

    private void assertInvalidQuery(Runnable action, String field) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BaseException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONTENT_QUERY_INVALID);
            assertThat(exception.getDetails()).containsEntry("field", field);
        });
    }
}
