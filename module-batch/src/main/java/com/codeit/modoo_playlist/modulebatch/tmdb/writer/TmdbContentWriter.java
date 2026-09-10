package com.codeit.modoo_playlist.modulebatch.tmdb.writer;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.persistence.TmdbContentMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TmdbContentWriter implements ItemWriter<TmdbSyncContent> {

    private final TmdbContentMapper contentMapper;

    @Override
    public void write(Chunk<? extends TmdbSyncContent> chunk) {
        for (TmdbSyncContent content : chunk) {
            contentMapper.upsertContent(content);
            contentMapper.upsertVideo(content.id(), content.video());

            if (content.replacePeople()) {
                contentMapper.deletePeople(content.id());
                if (!content.people().isEmpty()) {
                    contentMapper.insertPeople(content.id(), content.people());
                }
            }

            if (content.replaceTags()) {
                if (!content.tags().isEmpty()) {
                    contentMapper.insertTags(content.tags());
                }
                contentMapper.deleteMissingOpenApiTags(content.id(), content.tags());
                if (!content.tags().isEmpty()) {
                    contentMapper.upsertContentTags(content.id(), content.tags());
                }
            }
        }
    }
}
