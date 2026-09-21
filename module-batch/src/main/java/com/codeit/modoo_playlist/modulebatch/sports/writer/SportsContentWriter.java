package com.codeit.modoo_playlist.modulebatch.sports.writer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.codeit.modoo_playlist.infra.event.kafka.IndexKafkaPublisher;
import com.codeit.modoo_playlist.modulebatch.sports.model.SportsSyncContent;
import com.codeit.modoo_playlist.modulebatch.sports.persistence.SportsContentMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SportsContentWriter implements ItemWriter<SportsSyncContent> {

    private final SportsContentMapper contentMapper;
    private final IndexKafkaPublisher indexKafkaPublisher;

    @Override
    public void write(Chunk<? extends SportsSyncContent> chunk) {
        Set<UUID> contentIds = new LinkedHashSet<>();
        for (SportsSyncContent content : chunk) {
            contentMapper.upsertContent(content);
            String contentId = contentMapper.findContentIdBySourceId(content.sourceId());
            if (contentId == null) {
                throw new IllegalStateException(
                        "저장된 TheSportsDB 콘텐츠 ID를 찾을 수 없습니다: " + content.sourceId()
                );
            }
            contentIds.add(UUID.fromString(contentId));
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
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                contentIds.forEach(indexKafkaPublisher::publish);
            }
        });
    }
}
