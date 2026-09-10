package com.codeit.modoo_playlist.modulebatch.tmdb.processor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.content.type.VideoReleaseStatus;
import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.infra.client.tmdb.TmdbClient;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbCreditsResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbGenreResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbKeywordResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbMovieDetailResponse;
import com.codeit.modoo_playlist.infra.client.tmdb.dto.TmdbTvDetailResponse;
import com.codeit.modoo_playlist.modulebatch.tmdb.config.TmdbBatchProperties;
import com.codeit.modoo_playlist.modulebatch.tmdb.exception.TmdbInvalidContentException;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.ExistingTmdbContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;
import com.fasterxml.uuid.Generators;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TmdbSyncConverter {

    private static final int MAX_CAST_COUNT = 10;
    private static final int MAX_PERSON_NAME_LENGTH = 100;
    private static final int MAX_CHARACTER_NAME_LENGTH = 100;

    private final TmdbClient tmdbClient;
    private final TmdbBatchProperties properties;

    public TmdbSyncContent fromMovie(
            TmdbMovieDetailResponse detail,
            ExistingTmdbContent existing
    ) {
        validateRequired(detail == null ? null : detail.title(), detail == null ? null : detail.posterPath(), existing);

        List<TmdbSyncContent.Person> people = new ArrayList<>();
        addMovieDirector(people, detail.credits());
        addCast(people, detail.credits());

        return new TmdbSyncContent(
                contentId(existing),
                ContentType.MOVIE.name(),
                detail.title(),
                preserve(detail.overview(), existing == null ? null : existing.description()),
                preserve(tmdbClient.toImageUrl(detail.posterPath()), existing == null ? null : existing.thumbnailUrl()),
                Long.toString(detail.id()),
                preserve(detail.releaseDate(), existing == null ? null : existing.releaseDate()),
                preserve(first(detail.originCountry()), existing == null ? null : existing.originCountry()),
                new TmdbSyncContent.Video(
                        preserve(detail.runtime(), existing == null ? null : existing.runtimeMinutes()),
                        preserve(detail.belongsToCollection() == null ? null : detail.belongsToCollection().name(),
                                existing == null ? null : existing.collectionName()),
                        preserve(detail.imdbId(), existing == null ? null : existing.imdbId()),
                        movieStatus(detail.status(), detail.releaseDate()).name(),
                        null,
                        null,
                        preserve(detail.originalLanguage(), existing == null ? null : existing.originalLanguage()),
                        preserve(detail.popularity(), existing == null ? null : existing.popularity()),
                        preserve(detail.voteAverage(), existing == null ? null : existing.externalRating()),
                        preserve(detail.voteCount(), existing == null ? null : existing.externalRatingCount())
                ),
                List.copyOf(people),
                tags(detail.genres(), detail.keywords()),
                existing == null || detail.credits() != null,
                existing == null || tagSnapshotComplete(detail.genres(), detail.keywords())
        );
    }

    public TmdbSyncContent fromTv(
            TmdbTvDetailResponse detail,
            ExistingTmdbContent existing
    ) {
        validateRequired(detail == null ? null : detail.name(), detail == null ? null : detail.posterPath(), existing);

        List<TmdbSyncContent.Person> people = new ArrayList<>();
        addCreators(people, detail.createdBy());
        addCast(people, detail.credits());

        Integer runtime = detail.episodeRunTime() == null
                ? null
                : detail.episodeRunTime().stream().filter(value -> value != null && value > 0).findFirst().orElse(null);
        String imdbId = detail.externalIds() == null ? null : detail.externalIds().imdbId();

        return new TmdbSyncContent(
                contentId(existing),
                ContentType.TV.name(),
                detail.name(),
                preserve(detail.overview(), existing == null ? null : existing.description()),
                preserve(tmdbClient.toImageUrl(detail.posterPath()), existing == null ? null : existing.thumbnailUrl()),
                Long.toString(detail.id()),
                preserve(detail.firstAirDate(), existing == null ? null : existing.releaseDate()),
                preserve(first(detail.originCountry()), existing == null ? null : existing.originCountry()),
                new TmdbSyncContent.Video(
                        preserve(runtime, existing == null ? null : existing.runtimeMinutes()),
                        null,
                        preserve(imdbId, existing == null ? null : existing.imdbId()),
                        tvStatus(detail.status(), detail.firstAirDate()).name(),
                        preserve(detail.numberOfSeasons(), existing == null ? null : existing.numberOfSeasons()),
                        preserve(detail.numberOfEpisodes(), existing == null ? null : existing.numberOfEpisodes()),
                        preserve(detail.originalLanguage(), existing == null ? null : existing.originalLanguage()),
                        preserve(detail.popularity(), existing == null ? null : existing.popularity()),
                        preserve(detail.voteAverage(), existing == null ? null : existing.externalRating()),
                        preserve(detail.voteCount(), existing == null ? null : existing.externalRatingCount())
                ),
                List.copyOf(people),
                tags(detail.genres(), detail.keywords()),
                existing == null || detail.credits() != null,
                existing == null || tagSnapshotComplete(detail.genres(), detail.keywords())
        );
    }

    private void validateRequired(String title, String posterPath, ExistingTmdbContent existing) {
        if (title == null || title.isBlank()) {
            throw new TmdbInvalidContentException("TMDB 콘텐츠 제목이 없습니다.");
        }
        if (existing == null && (posterPath == null || posterPath.isBlank())) {
            throw new TmdbInvalidContentException("신규 TMDB 콘텐츠 포스터가 없습니다.");
        }
    }

    private void addMovieDirector(List<TmdbSyncContent.Person> people, TmdbCreditsResponse credits) {
        if (credits == null || credits.crew() == null) {
            return;
        }
        credits.crew().stream()
                .filter(crew -> "Director".equalsIgnoreCase(crew.job()))
                .findFirst()
                .ifPresent(director -> people.add(person(
                        "DIRECTOR", director.name(), null, 0, director.id(), director.profilePath()
                )));
    }

    private void addCreators(List<TmdbSyncContent.Person> people, List<TmdbTvDetailResponse.Creator> creators) {
        if (creators == null) {
            return;
        }
        creators.stream()
                .filter(creator -> creator.name() != null && !creator.name().isBlank())
                .forEach(creator -> people.add(person(
                        "CREATOR", creator.name(), null, people.size(), creator.id(), creator.profilePath()
                )));
    }

    private void addCast(List<TmdbSyncContent.Person> people, TmdbCreditsResponse credits) {
        if (credits == null || credits.cast() == null) {
            return;
        }
        credits.cast().stream()
                .filter(cast -> cast.name() != null && !cast.name().isBlank())
                .sorted(Comparator.comparingInt(TmdbCreditsResponse.Cast::order))
                .limit(MAX_CAST_COUNT)
                .forEach(cast -> people.add(person(
                        "ACTOR",
                        cast.name(),
                        cast.character(),
                        people.size(),
                        cast.id(),
                        cast.profilePath()
                )));
    }

    private TmdbSyncContent.Person person(
            String roleType,
            String name,
            String character,
            int order,
            long personId,
            String profilePath
    ) {
        return new TmdbSyncContent.Person(
                newId(), roleType,
                limitLength(name, MAX_PERSON_NAME_LENGTH),
                limitLength(character, MAX_CHARACTER_NAME_LENGTH),
                order,
                Long.toString(personId), tmdbClient.toImageUrl(profilePath)
        );
    }

    private String limitLength(String value, int maxLength) {
        if (value == null || value.codePointCount(0, value.length()) <= maxLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }

    private List<TmdbSyncContent.Tag> tags(
            List<TmdbGenreResponse> genres,
            TmdbKeywordResponse keywords
    ) {
        Map<String, TmdbSyncContent.Tag> tags = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (genres != null) {
            genres.stream()
                    .filter(genre -> validTagName(genre.name()))
                    .forEach(genre -> tags.putIfAbsent(
                            genre.name().trim(),
                            new TmdbSyncContent.Tag(newId(), genre.name().trim(), TagKind.GENRE.name())
                    ));
        }
        if (keywords != null) {
            keywords.items().stream()
                    .filter(keyword -> validTagName(keyword.name()))
                    .forEach(keyword -> tags.putIfAbsent(
                            keyword.name().trim(),
                            new TmdbSyncContent.Tag(newId(), keyword.name().trim(), TagKind.KEYWORD.name())
                    ));
        }
        return List.copyOf(tags.values());
    }

    private boolean validTagName(String name) {
        return name != null && !name.isBlank() && name.trim().length() <= 50;
    }

    private boolean tagSnapshotComplete(List<TmdbGenreResponse> genres, TmdbKeywordResponse keywords) {
        return genres != null
                && keywords != null
                && (keywords.keywords() != null || keywords.results() != null);
    }

    private VideoReleaseStatus movieStatus(String status, LocalDate releaseDate) {
        if ("Canceled".equalsIgnoreCase(status)) {
            return VideoReleaseStatus.CANCELED;
        }
        if (releaseDate != null && releaseDate.isAfter(today())) {
            return VideoReleaseStatus.UPCOMING;
        }
        return "Released".equalsIgnoreCase(status)
                ? VideoReleaseStatus.RELEASED
                : VideoReleaseStatus.UPCOMING;
    }

    private VideoReleaseStatus tvStatus(String status, LocalDate firstAirDate) {
        if ("Canceled".equalsIgnoreCase(status)) {
            return VideoReleaseStatus.CANCELED;
        }
        if ("Ended".equalsIgnoreCase(status)) {
            return VideoReleaseStatus.ENDED;
        }
        if ("Planned".equalsIgnoreCase(status)
                || "In Production".equalsIgnoreCase(status)
                || "Pilot".equalsIgnoreCase(status)) {
            return VideoReleaseStatus.UPCOMING;
        }
        if (firstAirDate != null && firstAirDate.isAfter(today())) {
            return VideoReleaseStatus.UPCOMING;
        }
        return "Returning Series".equalsIgnoreCase(status)
                ? VideoReleaseStatus.ONGOING
                : VideoReleaseStatus.ENDED;
    }

    private String contentId(ExistingTmdbContent existing) {
        return existing == null ? newId() : existing.id();
    }

    private String newId() {
        return Generators.timeBasedEpochGenerator().generate().toString();
    }

    private LocalDate today() {
        return LocalDate.now(java.time.ZoneId.of(properties.getZone()));
    }

    private String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private String preserve(String incoming, String existing) {
        return incoming == null || incoming.isBlank() ? existing : incoming;
    }

    private <T> T preserve(T incoming, T existing) {
        return incoming == null ? existing : incoming;
    }
}
