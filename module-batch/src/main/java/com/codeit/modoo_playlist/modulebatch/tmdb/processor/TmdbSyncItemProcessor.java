package com.codeit.modoo_playlist.modulebatch.tmdb.processor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.codeit.modoo_playlist.core.domain.content.type.VideoReleaseStatus;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.ExistingTmdbContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbFetchedContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbCandidate.MediaType;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.persistence.TmdbContentMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TmdbSyncItemProcessor implements ItemProcessor<TmdbFetchedContent, TmdbSyncContent> {

    private final MediaType mediaType;
    private final SyncMode syncMode;
    private final TmdbContentMapper contentMapper;
    private final TmdbSyncConverter converter;

    @Override
    public TmdbSyncContent process(TmdbFetchedContent fetched) {
        if (fetched.failure() != null) {
            BaseException exception = new BaseException(ErrorCode.TMDB_ITEM_FETCH_FAILED, fetched.failure());
            exception.addDetail("mediaType", fetched.candidate().mediaType());
            exception.addDetail("tmdbId", fetched.candidate().tmdbId());
            throw exception;
        }

        long tmdbId = fetched.candidate().tmdbId();
        ExistingTmdbContent existing = contentMapper.findBySourceId(
                mediaType.name(),
                Long.toString(tmdbId)
        );

        if (existing != null && (syncMode == SyncMode.POPULAR || existing.deletedAt() != null)) {
            return null;
        }

        TmdbSyncContent content = mediaType == MediaType.MOVIE
                ? converter.fromMovie(fetched.movieDetail(), existing)
                : converter.fromTv(fetched.tvDetail(), existing);

        if (syncMode == SyncMode.POPULAR
                && existing == null
                && VideoReleaseStatus.UPCOMING.name().equals(content.video().releaseStatus())) {
            return null;
        }
        if (existing != null && unchanged(existing, content)) {
            return null;
        }
        return content;
    }

    private boolean unchanged(ExistingTmdbContent existing, TmdbSyncContent incoming) {
        if (!sameMainFields(existing, incoming)) {
            return false;
        }
        if (incoming.replacePeople()
                && !samePeople(contentMapper.findPeopleByContentId(existing.id()), incoming.people())) {
            return false;
        }
        return !incoming.replaceTags()
                || sameTags(contentMapper.findTagsByContentId(existing.id()), incoming.tags());
    }

    private boolean sameMainFields(ExistingTmdbContent existing, TmdbSyncContent incoming) {
        TmdbSyncContent.Video video = incoming.video();
        return Objects.equals(existing.title(), incoming.title())
                && Objects.equals(existing.description(), incoming.description())
                && Objects.equals(existing.thumbnailUrl(), incoming.thumbnailUrl())
                && Objects.equals(existing.releaseDate(), incoming.releaseDate())
                && Objects.equals(existing.originCountry(), incoming.originCountry())
                && Objects.equals(existing.runtimeMinutes(), video.runtimeMinutes())
                && Objects.equals(existing.collectionName(), video.collectionName())
                && Objects.equals(existing.imdbId(), video.imdbId())
                && Objects.equals(existing.releaseStatus(), video.releaseStatus())
                && Objects.equals(existing.numberOfSeasons(), video.numberOfSeasons())
                && Objects.equals(existing.numberOfEpisodes(), video.numberOfEpisodes())
                && Objects.equals(existing.originalLanguage(), video.originalLanguage())
                && Objects.equals(existing.popularity(), video.popularity())
                && sameNumber(existing.externalRating(), video.externalRating())
                && Objects.equals(existing.externalRatingCount(), video.externalRatingCount());
    }

    private boolean samePeople(
            List<ExistingTmdbContent.Person> existing,
            List<TmdbSyncContent.Person> incoming
    ) {
        if (existing.size() != incoming.size()) {
            return false;
        }
        for (int index = 0; index < existing.size(); index++) {
            ExistingTmdbContent.Person left = existing.get(index);
            TmdbSyncContent.Person right = incoming.get(index);
            if (!Objects.equals(left.roleType(), right.roleType())
                    || !Objects.equals(left.personName(), right.personName())
                    || !Objects.equals(left.characterName(), right.characterName())
                    || left.displayOrder() != right.displayOrder()
                    || !Objects.equals(left.personId(), right.personId())
                    || !Objects.equals(left.personImg(), right.personImg())) {
                return false;
            }
        }
        return true;
    }

    private boolean sameTags(
            List<ExistingTmdbContent.Tag> existing,
            List<TmdbSyncContent.Tag> incoming
    ) {
        Set<String> incomingNames = new HashSet<>();
        for (TmdbSyncContent.Tag tag : incoming) {
            incomingNames.add(normalize(tag.name()));
        }

        Set<String> existingOpenApiNames = new HashSet<>();
        Set<String> existingOtherNames = new HashSet<>();
        for (ExistingTmdbContent.Tag tag : existing) {
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

    private boolean sameNumber(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public enum SyncMode {
        ACTIVE,
        POPULAR
    }
}
