package com.codeit.modoo_playlist.modulebatch.tmdb.writer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.persistence.TmdbContentMapper;

@ExtendWith(MockitoExtension.class)
class TmdbContentWriterTest {

    @Mock private TmdbContentMapper mapper;

    @Test
    void 콘텐츠_비디오_사람_태그를_정해진_순서로_저장한다() throws Exception {
        TmdbSyncContent content = content(true, true);
        when(mapper.findContentIdBySourceId("MOVIE", "10")).thenReturn("content-id");

        new TmdbContentWriter(mapper).write(new Chunk<>(List.of(content)));

        InOrder order = inOrder(mapper);
        order.verify(mapper).upsertContent(content);
        order.verify(mapper).findContentIdBySourceId("MOVIE", "10");
        order.verify(mapper).upsertVideo("content-id", content.video());
        order.verify(mapper).deletePeople("content-id");
        order.verify(mapper).insertPeople("content-id", content.people());
        order.verify(mapper).insertTags(content.tags());
        order.verify(mapper).decreaseMissingOpenApiTagCounts("content-id", content.tags());
        order.verify(mapper).deleteMissingOpenApiTags("content-id", content.tags());
        order.verify(mapper).increaseNewContentTagCounts("content-id", content.tags());
        order.verify(mapper).upsertContentTags("content-id", content.tags());
    }

    @Test
    void 불완전한_스냅샷이면_사람과_태그를_교체하지_않는다() throws Exception {
        TmdbSyncContent content = content(false, false);
        when(mapper.findContentIdBySourceId("MOVIE", "10")).thenReturn("content-id");

        new TmdbContentWriter(mapper).write(new Chunk<>(List.of(content)));

        verify(mapper, never()).deletePeople("content-id");
        verify(mapper, never()).insertTags(content.tags());
        verify(mapper, never()).deleteMissingOpenApiTags("content-id", content.tags());
    }

    @Test
    void upsert후_ID를_찾지_못하면_즉시_실패한다() {
        TmdbSyncContent content = content(true, true);
        when(mapper.findContentIdBySourceId("MOVIE", "10")).thenReturn(null);

        assertThatThrownBy(() -> new TmdbContentWriter(mapper).write(new Chunk<>(List.of(content))))
                .isInstanceOf(IllegalStateException.class);
        verify(mapper, never()).upsertVideo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private TmdbSyncContent content(boolean replacePeople, boolean replaceTags) {
        var video = new TmdbSyncContent.Video(null, null, null, "RELEASED",
                null, null, null, null, null, null);
        var person = new TmdbSyncContent.Person("p", "ACTOR", "name", null, 0, "1", null);
        var tag = new TmdbSyncContent.Tag("t", "Action", "GENRE");
        return new TmdbSyncContent("id", "MOVIE", "title", null, null, "10", null, null,
                video, List.of(person), List.of(tag), replacePeople, replaceTags);
    }
}
