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
            String contentId = contentMapper.findContentIdBySourceId(content.type(), content.sourceId());
            if (contentId == null) {
                throw new IllegalStateException("저장된 TMDB 콘텐츠 ID를 찾을 수 없습니다: " + content.sourceId());
            }
            contentMapper.upsertVideo(contentId, content.video());

            if (content.replacePeople()) {
                contentMapper.deletePeople(contentId);
                if (!content.people().isEmpty()) {
                    contentMapper.insertPeople(contentId, content.people());
                }
            }

            if (content.replaceTags()) {
                if (!content.tags().isEmpty()) {
                    contentMapper.insertTags(content.tags());
                }
                contentMapper.decreaseMissingOpenApiTagCounts(contentId, content.tags());
                contentMapper.deleteMissingOpenApiTags(contentId, content.tags());
                if (!content.tags().isEmpty()) {
                    contentMapper.increaseNewContentTagCounts(contentId, content.tags());
                    contentMapper.upsertContentTags(contentId, content.tags());
                }
            }
        }
    }
}
