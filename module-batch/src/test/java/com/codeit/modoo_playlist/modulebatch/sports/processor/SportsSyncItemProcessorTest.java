package com.codeit.modoo_playlist.modulebatch.sports.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEvent;
import com.codeit.modoo_playlist.modulebatch.sports.model.ExistingSportsContent;
import com.codeit.modoo_playlist.modulebatch.sports.model.SportsSyncContent;
import com.codeit.modoo_playlist.modulebatch.sports.persistence.SportsContentMapper;

@ExtendWith(MockitoExtension.class)
class SportsSyncItemProcessorTest {

    @Mock private SportsContentMapper mapper;
    @Mock private SportsSyncConverter converter;

    @Test
    void softDelete된_기존경기는_변환하지_않는다() throws Exception {
        SportsDbEvent event = event();
        when(mapper.findBySourceId("event-1")).thenReturn(existing(Instant.now()));

        assertThat(new SportsSyncItemProcessor(mapper, converter).process(event)).isNull();
        verify(converter, never()).convert(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 기존값과_태그까지_같으면_noOp한다() throws Exception {
        SportsDbEvent event = event();
        ExistingSportsContent existing = existing(null);
        SportsSyncContent converted = sync("title");
        when(mapper.findBySourceId("event-1")).thenReturn(existing);
        when(converter.convert(event, existing)).thenReturn(converted);
        when(mapper.findTagsByContentId("id"))
                .thenReturn(List.of(new ExistingSportsContent.Tag("축구", "OPENAPI")));

        assertThat(new SportsSyncItemProcessor(mapper, converter).process(event)).isNull();
    }

    @Test
    void 신규경기는_저장대상으로_반환한다() throws Exception {
        SportsDbEvent event = event();
        SportsSyncContent converted = sync("changed");
        when(mapper.findBySourceId("event-1")).thenReturn(null);
        when(converter.convert(event, null)).thenReturn(converted);

        assertThat(new SportsSyncItemProcessor(mapper, converter).process(event)).isSameAs(converted);
    }

    @Test
    void 기존경기의_값이_변경되면_저장대상으로_반환한다() throws Exception {
        SportsDbEvent event = event();
        ExistingSportsContent existing = existing(null);
        SportsSyncContent converted = sync("changed");
        when(mapper.findBySourceId("event-1")).thenReturn(existing);
        when(converter.convert(event, existing)).thenReturn(converted);

        assertThat(new SportsSyncItemProcessor(mapper, converter).process(event)).isSameAs(converted);
    }

    private SportsDbEvent event() {
        return new SportsDbEvent("event-1", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private ExistingSportsContent existing(Instant deletedAt) {
        return new ExistingSportsContent("id", "title", null, null, deletedAt, null, null,
                "Soccer", "League", "2026", "A", "B", null, "SCHEDULED",
                Instant.parse("2026-12-01T12:00:00Z"));
    }

    private SportsSyncContent sync(String title) {
        var sports = new SportsSyncContent.Sports("Soccer", "League", "2026", "A", "B",
                null, "SCHEDULED", Instant.parse("2026-12-01T12:00:00Z"));
        return new SportsSyncContent("id", title, null, null, "event-1", null, null,
                sports, List.of(new SportsSyncContent.Tag("tag", "축구", "GENRE")));
    }
}
