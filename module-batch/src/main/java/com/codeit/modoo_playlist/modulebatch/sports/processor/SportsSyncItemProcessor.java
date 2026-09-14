package com.codeit.modoo_playlist.modulebatch.sports.processor;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEvent;
import com.codeit.modoo_playlist.modulebatch.sports.model.ExistingSportsContent;
import com.codeit.modoo_playlist.modulebatch.sports.model.SportsSyncContent;
import com.codeit.modoo_playlist.modulebatch.sports.persistence.SportsContentMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SportsSyncItemProcessor implements ItemProcessor<SportsDbEvent, SportsSyncContent> {

    private final SportsContentMapper contentMapper;
    private final SportsSyncConverter converter;

    @Override
    public SportsSyncContent process(SportsDbEvent event) {
        ExistingSportsContent existing = contentMapper.findBySourceId(event.idEvent());
        if (existing != null && existing.deletedAt() != null) {
            return null;
        }

        SportsSyncContent content = converter.convert(event, existing);
        if (existing != null && unchanged(existing, content)) {
            return null;
        }
        return content;
    }

    private boolean unchanged(ExistingSportsContent existing, SportsSyncContent incoming) {
        SportsSyncContent.Sports sports = incoming.sports();
        return Objects.equals(existing.title(), incoming.title())
                && Objects.equals(existing.description(), incoming.description())
                && Objects.equals(existing.thumbnailUrl(), incoming.thumbnailUrl())
                && Objects.equals(existing.releaseDate(), incoming.releaseDate())
                && Objects.equals(existing.originCountry(), incoming.originCountry())
                && Objects.equals(existing.sportType(), sports.sportType())
                && Objects.equals(existing.league(), sports.league())
                && Objects.equals(existing.season(), sports.season())
                && Objects.equals(existing.homeTeam(), sports.homeTeam())
                && Objects.equals(existing.awayTeam(), sports.awayTeam())
                && Objects.equals(existing.venue(), sports.venue())
                && Objects.equals(existing.status(), sports.status())
                && Objects.equals(existing.kickoffAt(), sports.kickoffAt())
                && sameTags(contentMapper.findTagsByContentId(existing.id()), incoming.tags());
    }

    private boolean sameTags(
            List<ExistingSportsContent.Tag> existing,
            List<SportsSyncContent.Tag> incoming
    ) {
        Set<String> incomingNames = new HashSet<>();
        for (SportsSyncContent.Tag tag : incoming) {
            incomingNames.add(normalize(tag.name()));
        }

        Set<String> existingOpenApiNames = new HashSet<>();
        Set<String> existingOtherNames = new HashSet<>();
        for (ExistingSportsContent.Tag tag : existing) {
            if ("OPENAPI".equals(tag.source())) {
                existingOpenApiNames.add(normalize(tag.name()));
            } else {
                existingOtherNames.add(normalize(tag.name()));
            }
        }

        Set<String> expectedOpenApiNames = new HashSet<>(incomingNames);
        expectedOpenApiNames.removeAll(existingOtherNames);
        return existingOpenApiNames.equals(expectedOpenApiNames);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
