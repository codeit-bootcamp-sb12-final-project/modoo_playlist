package com.codeit.modoo_playlist.modulebatch.sports.writer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import com.codeit.modoo_playlist.modulebatch.sports.model.SportsSyncContent;
import com.codeit.modoo_playlist.modulebatch.sports.persistence.SportsContentMapper;

@ExtendWith(MockitoExtension.class)
class SportsContentWriterTest {

    @Mock private SportsContentMapper mapper;

    @Test
    void 스포츠와_태그를_정합성_순서대로_저장한다() throws Exception {
        SportsSyncContent content = content();
        when(mapper.findContentIdBySourceId("event-1")).thenReturn("content-id");

        new SportsContentWriter(mapper).write(new Chunk<>(List.of(content)));

        InOrder order = inOrder(mapper);
        order.verify(mapper).upsertContent(content);
        order.verify(mapper).findContentIdBySourceId("event-1");
        order.verify(mapper).upsertSports("content-id", content.sports());
        order.verify(mapper).insertTags(content.tags());
        order.verify(mapper).decreaseMissingOpenApiTagCounts("content-id", content.tags());
        order.verify(mapper).deleteMissingOpenApiTags("content-id", content.tags());
        order.verify(mapper).increaseNewContentTagCounts("content-id", content.tags());
        order.verify(mapper).upsertContentTags("content-id", content.tags());
    }

    @Test
    void upsert후_ID가_없으면_연관정보를_저장하지_않는다() {
        SportsSyncContent content = content();
        when(mapper.findContentIdBySourceId("event-1")).thenReturn(null);

        assertThatThrownBy(() -> new SportsContentWriter(mapper).write(new Chunk<>(List.of(content))))
                .isInstanceOf(IllegalStateException.class);
        verify(mapper).upsertContent(content);
        verify(mapper).findContentIdBySourceId("event-1");
        verifyNoMoreInteractions(mapper);
    }

    private SportsSyncContent content() {
        var sports = new SportsSyncContent.Sports("Soccer", "Premier League", "2026",
                "A", "B", null, "SCHEDULED", Instant.parse("2026-09-14T12:00:00Z"));
        return new SportsSyncContent("id", "A vs B", null, null, "event-1", null, null,
                sports, List.of(new SportsSyncContent.Tag("tag", "축구", "GENRE")));
    }
}
