package com.codeit.modoo_playlist.modulebatch.sports.processor;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.codeit.modoo_playlist.core.domain.content.type.SportsStatus;
import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.infra.client.sportsdb.dto.SportsDbEvent;
import com.codeit.modoo_playlist.modulebatch.sports.config.SportsBatchProperties;
import com.codeit.modoo_playlist.modulebatch.sports.model.ExistingSportsContent;
import com.codeit.modoo_playlist.modulebatch.sports.model.SportsSyncContent;
import com.fasterxml.uuid.Generators;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SportsSyncConverter {

    private final SportsBatchProperties properties;

    public SportsSyncContent convert(SportsDbEvent event, ExistingSportsContent existing) {
        if (event == null || blank(event.idEvent())) {
            throw invalidEvent("idEvent", null, null);
        }

        String sportType = requiredOrExisting(event.strSport(), existing == null ? null : existing.sportType(), "종목");
        String league = requiredOrExisting(event.strLeague(), existing == null ? null : existing.league(), "리그");
        String homeTeam = requiredOrExisting(event.strHomeTeam(), existing == null ? null : existing.homeTeam(), "홈팀");
        String awayTeam = requiredOrExisting(event.strAwayTeam(), existing == null ? null : existing.awayTeam(), "원정팀");
        Instant kickoffAt = resolveKickoff(event.strTimestamp(), existing);

        String title = trimmed(event.strEvent());
        if (blank(title)) {
            title = homeTeam + " vs " + awayTeam;
        }

        String thumbnail = firstPresent(
                event.strThumb(),
                event.strLeagueBadge(),
                event.strHomeTeamBadge(),
                existing == null ? null : existing.thumbnailUrl()
        );
        SportsStatus status = resolveStatus(event, existing, sportType, kickoffAt);
        String season = preserve(event.strSeason(), existing == null ? null : existing.season());
        String venue = preserve(event.strVenue(), existing == null ? null : existing.venue());
        String country = preserve(event.strCountry(), existing == null ? null : existing.originCountry());

        SportsSyncContent.Sports sports = new SportsSyncContent.Sports(
                limit(sportType, 50),
                limit(league, 100),
                limit(season, 20),
                limit(homeTeam, 100),
                limit(awayTeam, 100),
                limit(venue, 100),
                status.name(),
                kickoffAt
        );

        return new SportsSyncContent(
                existing == null ? newId() : existing.id(),
                limit(title, 255),
                existing == null ? null : existing.description(),
                limit(thumbnail, 500),
                event.idEvent().trim(),
                LocalDate.ofInstant(kickoffAt, ZoneId.of(properties.getZone())),
                limit(country, 20),
                sports,
                tags(sports)
        );
    }

    private Instant resolveKickoff(String timestamp, ExistingSportsContent existing) {
        if (blank(timestamp)) {
            if (existing != null && existing.kickoffAt() != null) {
                return existing.kickoffAt();
            }
            throw invalidEvent("strTimestamp", null, null);
        }
        String value = timestamp.trim();
        try {
            return Instant.parse(value);
        } catch (DateTimeException ignored) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeException ignoredAgain) {
                try {
                    return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
                } catch (DateTimeException exception) {
                    throw invalidEvent("strTimestamp", value, exception);
                }
            }
        }
    }

    private SportsStatus resolveStatus(
            SportsDbEvent event,
            ExistingSportsContent existing,
            String sportType,
            Instant kickoffAt
    ) {
        if ("yes".equalsIgnoreCase(trimmed(event.strPostponed()))) {
            return SportsStatus.POSTPONED;
        }

        String raw = normalizeStatus(event.strStatus());
        if (contains(raw, "CANC", "CANCELED", "CANCELLED", "MATCH_CANCELED", "MATCH_CANCELLED")) {
            return SportsStatus.CANCELED;
        }
        if (contains(raw, "PST", "POST", "POSTPONED", "SUSP", "SUSPENDED",
                "INT", "INTERRUPTED", "ABD", "ABANDONED", "MATCH_POSTPONED")) {
            return SportsStatus.POSTPONED;
        }
        if (contains(raw, "FT", "AET", "PEN", "AWD", "AWARDED", "WO",
                "WALKOVER", "MATCH_FINISHED", "GAME_FINISHED", "FULL_TIME", "FINISHED")) {
            return SportsStatus.FINISHED;
        }
        if (contains(raw, "1H", "HT", "2H", "ET", "BT", "LIVE", "IN_PLAY", "HALF_TIME")) {
            return SportsStatus.LIVE;
        }
        if (contains(raw, "NS", "TBD", "NOT_STARTED", "SCHEDULED")) {
            return calculatedStatus(sportType, kickoffAt);
        }
        if (blank(raw) && existing != null && SportsStatus.POSTPONED.name().equals(existing.status())) {
            return SportsStatus.POSTPONED;
        }

        return calculatedStatus(sportType, kickoffAt);
    }

    private SportsStatus calculatedStatus(String sportType, Instant kickoffAt) {
        Instant now = Instant.now();
        if (now.isBefore(kickoffAt)) {
            return SportsStatus.SCHEDULED;
        }
        return now.isBefore(kickoffAt.plusSeconds(properties.durationMinutes(sportType) * 60L))
                ? SportsStatus.LIVE
                : SportsStatus.FINISHED;
    }

    private List<SportsSyncContent.Tag> tags(SportsSyncContent.Sports sports) {
        List<SportsSyncContent.Tag> tags = new ArrayList<>();
        addTag(tags, localizedSportName(sports.sportType()), TagKind.GENRE);
        addTag(tags, sports.league(), TagKind.KEYWORD);
        addTag(tags, sports.homeTeam(), TagKind.KEYWORD);
        addTag(tags, sports.awayTeam(), TagKind.KEYWORD);
        return List.copyOf(tags);
    }

    private void addTag(List<SportsSyncContent.Tag> tags, String name, TagKind kind) {
        String value = trimmed(name);
        if (!blank(value)
                && value.length() <= 50
                && tags.stream().noneMatch(tag -> tag.name().equalsIgnoreCase(value))) {
            tags.add(new SportsSyncContent.Tag(newId(), value, kind.name()));
        }
    }

    private String localizedSportName(String sportType) {
        String normalized = sportType.toLowerCase(Locale.ROOT);
        if (normalized.contains("soccer") || normalized.contains("football")) {
            return "축구";
        }
        if (normalized.contains("basketball")) {
            return "농구";
        }
        if (normalized.contains("baseball")) {
            return "야구";
        }
        return sportType;
    }

    private String requiredOrExisting(String incoming, String existing, String fieldName) {
        String value = preserve(incoming, existing);
        if (blank(value)) {
            throw invalidEvent(fieldName, null, null);
        }
        return value;
    }

    private BaseException invalidEvent(String field, String value, Throwable cause) {
        BaseException exception = cause == null
                ? new BaseException(ErrorCode.SPORTSDB_EVENT_INVALID)
                : new BaseException(ErrorCode.SPORTSDB_EVENT_INVALID, cause);
        exception.addDetail("field", field);
        if (value != null) {
            exception.addDetail("value", value);
        }
        return exception;
    }

    private String preserve(String incoming, String existing) {
        return blank(incoming) ? existing : incoming.trim();
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (!blank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeStatus(String value) {
        return blank(value)
                ? null
                : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private boolean contains(String value, String... candidates) {
        if (value == null) {
            return false;
        }
        for (String candidate : candidates) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.codePointCount(0, value.length()) <= maxLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }

    private String trimmed(String value) {
        return value == null ? null : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String newId() {
        return Generators.timeBasedEpochGenerator().generate().toString();
    }
}
