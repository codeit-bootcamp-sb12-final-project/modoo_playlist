package com.codeit.modoo_playlist.modulebatch.tmdb.writer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

import com.codeit.modoo_playlist.infra.event.kafka.IndexKafkaPublisher;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.persistence.TmdbContentMapper;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class TmdbContentWriterTest {
    private static final String CONTENT_ID = "019ed8a0-0000-7000-8000-000000000002";

    @Mock private TmdbContentMapper mapper;
    @Mock private IndexKafkaPublisher indexKafkaPublisher;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void 콘텐츠_비디오_사람_태그를_정해진_순서로_저장한다() throws Exception {
        TmdbSyncContent content = content(true, true);
        when(mapper.findContentIdBySourceId("MOVIE", "10")).thenReturn(CONTENT_ID);

        new TmdbContentWriter(mapper, indexKafkaPublisher).write(new Chunk<>(List.of(content)));

        verifyNoInteractions(indexKafkaPublisher);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(indexKafkaPublisher).publish(UUID.fromString(CONTENT_ID));


        InOrder order = inOrder(mapper);
        order.verify(mapper).upsertContent(content);
        order.verify(mapper).findContentIdBySourceId("MOVIE", "10");
        order.verify(mapper).upsertVideo(CONTENT_ID, content.video());
        order.verify(mapper).deletePeople(CONTENT_ID);
        order.verify(mapper).insertPeople(CONTENT_ID, content.people());
        order.verify(mapper).insertTags(content.tags());
        order.verify(mapper).decreaseMissingOpenApiTagCounts(CONTENT_ID, content.tags());
        order.verify(mapper).deleteMissingOpenApiTags(CONTENT_ID, content.tags());
        order.verify(mapper).increaseNewContentTagCounts(CONTENT_ID, content.tags());
        order.verify(mapper).upsertContentTags(CONTENT_ID, content.tags());
    }

    @Test
    void 불완전한_스냅샷이면_사람과_태그를_교체하지_않는다() throws Exception {
        TmdbSyncContent content = content(false, false);
        when(mapper.findContentIdBySourceId("MOVIE", "10")).thenReturn(CONTENT_ID);

        new TmdbContentWriter(mapper, indexKafkaPublisher).write(new Chunk<>(List.of(content)));

        verify(mapper, never()).deletePeople(CONTENT_ID);
        verify(mapper, never()).insertPeople(CONTENT_ID, content.people());
        verify(mapper, never()).insertTags(content.tags());
        verify(mapper, never()).decreaseMissingOpenApiTagCounts(CONTENT_ID, content.tags());
        verify(mapper, never()).deleteMissingOpenApiTags(CONTENT_ID, content.tags());
        verify(mapper, never()).increaseNewContentTagCounts(CONTENT_ID, content.tags());
        verify(mapper, never()).upsertContentTags(CONTENT_ID, content.tags());
    }

    @Test
    void upsert후_ID를_찾지_못하면_즉시_실패한다() {
        TmdbSyncContent content = content(true, true);
        when(mapper.findContentIdBySourceId("MOVIE", "10")).thenReturn(null);

        assertThatThrownBy(() -> new TmdbContentWriter(mapper, indexKafkaPublisher).write(new Chunk<>(List.of(content))))
                .isInstanceOf(IllegalStateException.class);
        verify(mapper).upsertContent(content);
        verify(mapper).findContentIdBySourceId("MOVIE", "10");
        verifyNoMoreInteractions(mapper);
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
