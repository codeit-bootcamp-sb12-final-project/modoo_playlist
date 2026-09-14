package com.codeit.modoo_playlist.modulebatch.sports.writer;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.codeit.modoo_playlist.modulebatch.sports.model.SportsSyncContent;
import com.codeit.modoo_playlist.modulebatch.sports.persistence.SportsContentMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SportsContentWriter implements ItemWriter<SportsSyncContent> {

    private final SportsContentMapper contentMapper;

    @Override
    public void write(Chunk<? extends SportsSyncContent> chunk) {
        for (SportsSyncContent content : chunk) {
            contentMapper.upsertContent(content);
            String contentId = contentMapper.findContentIdBySourceId(content.sourceId());
            if (contentId == null) {
                throw new IllegalStateException(
                        "저장된 TheSportsDB 콘텐츠 ID를 찾을 수 없습니다: " + content.sourceId()
                );
            }
            contentMapper.upsertSports(contentId, content.sports());

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
