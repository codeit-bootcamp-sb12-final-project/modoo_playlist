package com.codeit.modoo_playlist.moduleapi.domain.content.service.impl;

import com.codeit.modoo_playlist.moduleapi.domain.search.event.ContentIndexRequestedEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentPerson;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentSports;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentVideo;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.content.type.SportsStatus;
import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.mapper.ContentMapper;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentPersonRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentSportsRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentVideoRepository;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentListCondition.SortDirection;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentListCondition.SortType;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentQueryPage.ContentItem;
import com.codeit.modoo_playlist.moduleapi.domain.content.service.ContentService;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageStorage;
import com.codeit.modoo_playlist.moduleapi.domain.image.storage.ImageCategory;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.repository.UserContentInteractionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.tag.service.TagService;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentPersonRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentSportsRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentVideoRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentDetailResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentListItemResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentServiceImpl implements ContentService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MOVIE_GENRE_LIMIT = 4;
    private static final int TV_GENRE_LIMIT = 3;
    private static final String DEFAULT_SORT_BY = "watcherCount";
    private static final String DEFAULT_SORT_DIRECTION = "DESCENDING";
    private static final Set<InteractionType> REACTION_TYPES = EnumSet.of(
            InteractionType.LIKE,
            InteractionType.DISLIKE,
            InteractionType.NOT_INTERESTED
    );

    private final ContentRepository contentRepository;
    private final ContentTagRepository contentTagRepository;
    private final ContentVideoRepository contentVideoRepository;
    private final ContentSportsRepository contentSportsRepository;
    private final ContentPersonRepository contentPersonRepository;
    private final UserContentInteractionRepository userContentInteractionRepository;
    private final TagService tagService;
    private final ContentMapper contentMapper;
    private final ImageStorage imageStorage;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ContentCursorResponse getContents(ContentListRequest request, UUID userId) {
        ResolvedQuery resolvedQuery = resolveQuery(request);
        ContentQueryPage page = contentRepository.findAllByCondition(resolvedQuery.condition(), userId);
        List<Content> contents = page.contents().stream()
                .map(ContentItem::content)
                .toList();
        Map<UUID, List<String>> tagsByContentId = loadDisplayTagsByContentId(contents);

        List<ContentListItemResponse> data = page.contents().stream()
                .map(item -> contentMapper.toListItem(
                        item.content(),
                        tagsByContentId.getOrDefault(item.content().getId(), List.of()),
                        item.watcherCount()
                ))
                .toList();

        return contentMapper.toCursorResponse(
                page,
                data,
                resolvedQuery.sortBy(),
                resolvedQuery.sortDirection()
        );
    }

    @Override
    public ContentDetailResponse getContent(UUID contentId, UUID userId) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BaseException(ErrorCode.CONTENT_NOT_FOUND));
        InteractionType myReaction = userContentInteractionRepository
                .findAllByUserIdAndContentIdAndTypeInOrderByUpdatedAtDescIdDesc(
                        userId, contentId, REACTION_TYPES)
                .stream()
                .findFirst()
                .map(interaction -> interaction.getType())
                .orElse(null);

        return createDetailResponse(content, myReaction);
    }

    @Override
    @Transactional
    public ContentDetailResponse createContent(
            ContentCreateRequest request,
            MultipartFile thumbnail
    ) {
        ContentType contentType = toContentType(request.type());
        validateReleaseDate(request.releaseDate());
        validateCreateDetails(contentType, request.video(), request.sports(), request.people());
        Content content = Content.builder()
                .type(contentType)
                .title(request.title())
                .description(request.description())
                .thumbnailUrl(storeThumbnailIfPresent(thumbnail))
                .releaseDate(request.releaseDate())
                .originCountry(request.originCountry())
                .build();
        contentRepository.save(content);
        syncContentTags(content, request.tags());
        syncSubtype(content, request.video(), request.sports(), true);
        if (content.getType() != ContentType.SPORT && request.people() != null) {
            replacePeople(content, request.people());
        }

        eventPublisher.publishEvent(new ContentIndexRequestedEvent(content.getId()));

        return createDetailResponse(content);
    }

    @Override
    @Transactional
    public ContentDetailResponse updateContent(
            UUID contentId,
            ContentUpdateRequest request,
            MultipartFile thumbnail
    ) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BaseException(ErrorCode.CONTENT_NOT_FOUND));
        validateReleaseDate(request.releaseDate());
        validateUpdateDetails(content.getType(), request.video(), request.sports(), request.people());

        String previousThumbnailUrl = content.getThumbnailUrl();
        String newThumbnailUrl = storeThumbnailIfPresent(thumbnail);
        content.update(
                request.title(),
                request.description(),
                newThumbnailUrl
        );
        registerPreviousThumbnailCleanup(previousThumbnailUrl, newThumbnailUrl);
        content.updateMetadata(request.releaseDate(), request.originCountry());
        if (request.tags() != null) {
            syncContentTags(content, request.tags());
        }
        syncSubtype(content, request.video(), request.sports(), false);
        if (request.people() != null) {
            replacePeople(content, request.people());
        }

        eventPublisher.publishEvent(new ContentIndexRequestedEvent(content.getId()));

        return createDetailResponse(content);
    }

    @Override
    @Transactional
    public void deleteContent(UUID contentId) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BaseException(ErrorCode.CONTENT_NOT_FOUND));
        removeAllContentTags(content);
        content.softDelete();

        eventPublisher.publishEvent(new ContentIndexRequestedEvent(content.getId()));
    }

    private void removeAllContentTags(Content content) {
        List<ContentTag> contentTags = contentTagRepository
                .findAllWithTagByContentIds(List.of(content.getId()));
        contentTagRepository.deleteAll(contentTags);
        contentTagRepository.decreaseTagContentCounts(contentTags.stream()
                .map(contentTag -> contentTag.getTag().getId())
                .toList());
        contentTagRepository.flush();
    }

    private ContentDetailResponse createDetailResponse(Content content) {
        return createDetailResponse(content, null);
    }

    private ContentDetailResponse createDetailResponse(
            Content content,
            InteractionType myReaction
    ) {
        UUID contentId = content.getId();
        List<String> tags = loadDisplayTagsByContentId(List.of(content))
                .getOrDefault(contentId, List.of());
        long watcherCount = contentRepository.countCurrentWatchers(contentId);
        List<ContentPerson> people = contentPersonRepository
                .findAllByContent_IdOrderByDisplayOrderAsc(contentId);

        ContentVideo video = null;
        ContentSports sports = null;

        if (content.getType() == ContentType.SPORT) {
            sports = contentSportsRepository.findById(contentId).orElse(null);
        } else {
            video = contentVideoRepository.findById(contentId).orElse(null);
        }

        return contentMapper.toDetail(
                content,
                tags,
                watcherCount,
                video,
                sports,
                people,
                myReaction
        );
    }

    private void syncContentTags(Content content, List<String> requestedTags) {
        if (requestedTags == null) {
            return;
        }
        List<Tag> desiredTags = tagService.getOrCreateTags(requestedTags);
        List<ContentTag> currentContentTags = contentTagRepository
                .findAllWithTagByContentIds(List.of(content.getId()));
        Map<UUID, ContentTag> currentByTagId = currentContentTags.stream()
                .collect(Collectors.toMap(
                        contentTag -> contentTag.getTag().getId(),
                        contentTag -> contentTag
                ));
        Set<UUID> desiredTagIds = desiredTags.stream()
                .map(Tag::getId)
                .collect(Collectors.toSet());

        List<ContentTag> removedContentTags = currentContentTags.stream()
                .filter(contentTag -> !desiredTagIds.contains(contentTag.getTag().getId()))
                .toList();
        contentTagRepository.deleteAll(removedContentTags);
        contentTagRepository.decreaseTagContentCounts(removedContentTags.stream()
                .map(contentTag -> contentTag.getTag().getId())
                .toList());

        List<ContentTag> addedContentTags = new ArrayList<>();
        for (Tag tag : desiredTags) {
            if (!currentByTagId.containsKey(tag.getId())) {
                addedContentTags.add(ContentTag.builder()
                        .id(new ContentTagId(content.getId(), tag.getId()))
                        .content(content)
                        .tag(tag)
                        .source(null)
                        .build());
            }
        }
        contentTagRepository.saveAll(addedContentTags);
        contentTagRepository.increaseTagContentCounts(addedContentTags.stream()
                .map(contentTag -> contentTag.getTag().getId())
                .toList());
        contentTagRepository.flush();
    }

    private void validateCreateDetails(ContentType type, ContentVideoRequest video,
            ContentSportsRequest sports, List<ContentPersonRequest> people) {
        if (type == ContentType.SPORT) {
            if (video != null || people != null || sports == null
                    || blank(sports.sportType()) || blank(sports.league())
                    || blank(sports.homeTeam()) || blank(sports.awayTeam())
                    || sports.kickoffAt() == null) {
                throw new BaseException(ErrorCode.CONTENT_DETAIL_INVALID);
            }
            return;
        }
        if (sports != null) {
            throw new BaseException(ErrorCode.CONTENT_DETAIL_INVALID);
        }
    }

    private void validateUpdateDetails(ContentType type, ContentVideoRequest video,
            ContentSportsRequest sports, List<ContentPersonRequest> people) {
        if (type == ContentType.SPORT) {
            if (video != null || people != null) {
                throw new BaseException(ErrorCode.CONTENT_DETAIL_INVALID);
            }
            if (sports != null && (blank(sports.sportType()) || blank(sports.league())
                    || blank(sports.homeTeam()) || blank(sports.awayTeam())
                    || sports.kickoffAt() == null)) {
                throw new BaseException(ErrorCode.CONTENT_DETAIL_INVALID);
            }
        } else if (sports != null) {
            throw new BaseException(ErrorCode.CONTENT_DETAIL_INVALID);
        }
    }

    private void syncSubtype(Content content, ContentVideoRequest videoRequest,
            ContentSportsRequest sportsRequest, boolean createMissingSports) {
        if (content.getType() == ContentType.SPORT) {
            if (sportsRequest == null) {
                return;
            }
            ContentSports sports = contentSportsRepository.findById(content.getId())
                    .orElseGet(() -> {
                        if (!createMissingSports) {
                            throw new BaseException(ErrorCode.CONTENT_DETAIL_INVALID);
                        }
                        return ContentSports.builder().content(content).build();
                    });
            SportsStatus status = sportsRequest.status() != null
                    ? sportsRequest.status()
                    : sports.getStatus() != null ? sports.getStatus() : SportsStatus.SCHEDULED;
            sports.update(sportsRequest.sportType(), sportsRequest.league(), sportsRequest.season(),
                    sportsRequest.homeTeam(), sportsRequest.awayTeam(), sportsRequest.venue(),
                    status, sportsRequest.kickoffAt());
            contentSportsRepository.save(sports);
            return;
        }
        if (videoRequest == null) {
            return;
        }
        ContentVideo video = contentVideoRepository.findById(content.getId())
                .orElseGet(() -> ContentVideo.builder()
                        .content(content).build());
        video.update(
                valueOrCurrent(videoRequest.runtimeMinutes(), video.getRuntimeMinutes()),
                valueOrCurrent(videoRequest.collectionName(), video.getCollectionName()),
                valueOrCurrent(videoRequest.imdbId(), video.getImdbId()),
                valueOrCurrent(videoRequest.releaseStatus(), video.getReleaseStatus()),
                valueOrCurrent(videoRequest.numberOfSeasons(), video.getNumberOfSeasons()),
                valueOrCurrent(videoRequest.numberOfEpisodes(), video.getNumberOfEpisodes()),
                valueOrCurrent(videoRequest.originalLanguage(), video.getOriginalLanguage()),
                valueOrCurrent(videoRequest.popularity(), video.getPopularity()),
                valueOrCurrent(videoRequest.externalRating(), video.getExternalRating()),
                valueOrCurrent(videoRequest.externalRatingCount(), video.getExternalRatingCount())
        );
        contentVideoRepository.save(video);
    }

    private void replacePeople(Content content, List<ContentPersonRequest> requests) {
        contentPersonRepository.deleteAllByContent_Id(content.getId());
        List<ContentPerson> people = java.util.stream.IntStream.range(0, requests.size())
                .mapToObj(index -> toPerson(content, requests.get(index), index))
                .toList();
        contentPersonRepository.saveAll(people);
    }

    private ContentPerson toPerson(Content content, ContentPersonRequest request, int displayOrder) {
        return ContentPerson.builder()
                .content(content)
                .roleType(blank(request.roleType()) ? "CAST" : request.roleType())
                .personName(request.personName())
                .characterName(request.characterName())
                .displayOrder(displayOrder)
                .personId(request.personId())
                .personImg(request.personImg())
                .build();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private <T> T valueOrCurrent(T requestedValue, T currentValue) {
        return requestedValue != null ? requestedValue : currentValue;
    }

    private void validateReleaseDate(LocalDate releaseDate) {
        if (releaseDate != null && (releaseDate.getYear() < 1000 || releaseDate.getYear() > 9999)) {
            throw new BaseException(ErrorCode.CONTENT_DETAIL_INVALID);
        }
    }

    private String storeThumbnailIfPresent(MultipartFile thumbnail) {
        if (thumbnail == null || thumbnail.isEmpty()) {
            return null;
        }
        try {
            String thumbnailUrl = imageStorage.store(thumbnail, ImageCategory.CONTENT_THUMBNAIL);
            registerThumbnailRollbackCleanup(thumbnailUrl);
            return thumbnailUrl;
        } catch (IOException exception) {
            throw new BaseException(ErrorCode.FILE_SAVE_FAILED, exception);
        }
    }

    private void registerThumbnailRollbackCleanup(String thumbnailUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    imageStorage.delete(thumbnailUrl);
                } catch (IOException ignored) {
                    // 원래 저장 실패 예외를 유지한다.
                }
            }
        });
    }

    private void registerPreviousThumbnailCleanup(String previousThumbnailUrl, String newThumbnailUrl) {
        if (previousThumbnailUrl == null || newThumbnailUrl == null
                || Objects.equals(previousThumbnailUrl, newThumbnailUrl)
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    return;
                }
                try {
                    imageStorage.delete(previousThumbnailUrl);
                } catch (IOException ignored) {
                }
            }
        });
    }

    private Map<UUID, List<String>> loadDisplayTagsByContentId(List<Content> contents) {
        if (contents.isEmpty()) {
            return Map.of();
        }

        List<UUID> contentIds = contents.stream()
                .map(Content::getId)
                .toList();
        Map<UUID, List<Tag>> tagsByContentId = contentTagRepository
                .findAllWithTagByContentIds(contentIds).stream()
                .collect(Collectors.groupingBy(
                        contentTag -> contentTag.getId().getContentId(),
                        Collectors.mapping(ContentTag::getTag, Collectors.toList())
                ));
        Map<UUID, ContentSports> sportsByContentId = contentSportsRepository
                .findAllById(contents.stream()
                        .filter(content -> content.getType() == ContentType.SPORT)
                        .map(Content::getId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(ContentSports::getContentId, sports -> sports));

        return contents.stream()
                .collect(Collectors.toMap(
                        Content::getId,
                        content -> displayTags(
                                content,
                                tagsByContentId.getOrDefault(content.getId(), List.of()),
                                sportsByContentId.get(content.getId())
                        )
                ));
    }

    private List<String> displayTags(Content content, List<Tag> tags, ContentSports sports) {
        if (content.getSource() == null) {
            return tags.stream()
                    .map(Tag::getName)
                    .toList();
        }

        List<String> genres = tags.stream()
                .filter(tag -> tag.getKind() == TagKind.GENRE)
                .map(Tag::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return switch (content.getType()) {
            case MOVIE -> genres.stream().limit(MOVIE_GENRE_LIMIT).toList();
            case TV -> tvDisplayTags(genres);
            case SPORT -> sportsDisplayTags(genres, sports);
        };
    }

    private List<String> tvDisplayTags(List<String> genres) {
        boolean animation = genres.stream().anyMatch(this::isAnimationGenre);
        String subtype = animation ? "애니메이션" : "드라마";
        List<String> displayTags = new ArrayList<>();
        displayTags.add(subtype);
        genres.stream()
                .filter(genre -> animation ? !isAnimationGenre(genre) : !isDramaGenre(genre))
                .limit(TV_GENRE_LIMIT)
                .forEach(displayTags::add);
        return List.copyOf(displayTags);
    }

    private List<String> sportsDisplayTags(List<String> genres, ContentSports sports) {
        if (sports == null) {
            return genres.stream().limit(MOVIE_GENRE_LIMIT).toList();
        }

        List<String> displayTags = new ArrayList<>();
        String sportType = sports.getSportType();
        if ((sportType == null || sportType.isBlank()) && !genres.isEmpty()) {
            sportType = genres.get(0);
        }
        addDisplayTag(displayTags, sportType);
        addDisplayTag(displayTags, sports.getLeague());
        addDisplayTag(displayTags, sports.getHomeTeam());
        addDisplayTag(displayTags, sports.getAwayTeam());
        return List.copyOf(displayTags);
    }

    private void addDisplayTag(List<String> displayTags, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        boolean duplicated = displayTags.stream().anyMatch(value::equalsIgnoreCase);
        if (!duplicated) {
            displayTags.add(value);
        }
    }

    private boolean isAnimationGenre(String genre) {
        return "animation".equalsIgnoreCase(genre) || "애니메이션".equals(genre);
    }

    private boolean isDramaGenre(String genre) {
        return "drama".equalsIgnoreCase(genre) || "드라마".equals(genre);
    }

    private ResolvedQuery resolveQuery(ContentListRequest request) {
        String requestedSortBy = defaultIfBlank(request.sortBy(), DEFAULT_SORT_BY);
        String sortDirection = defaultIfBlank(request.sortDirection(), DEFAULT_SORT_DIRECTION);

        SortType sortType = switch (requestedSortBy) {
            case "watcherCount" -> SortType.WATCHER_COUNT;
            case "createdAt" -> SortType.CREATED_AT;
            case "rate", "averageRating" -> SortType.AVERAGE_RATING;
            case "recommended" -> SortType.RECOMMENDED;
            default -> throw invalidValue(ErrorCode.CONTENT_SORT_INVALID, "sortBy", requestedSortBy);
        };

        ContentListCondition condition = new ContentListCondition(
                toContentType(request.typeEqual()),
                request.keywordLike(),
                request.tagsIn(),
                request.cursor(),
                request.idAfter(),
                request.limit() == null ? DEFAULT_LIMIT : request.limit(),
                sortType,
                toSortDirection(sortDirection)
        );

        return new ResolvedQuery(condition, requestedSortBy, sortDirection);
    }

    private ContentType toContentType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }

        return switch (type) {
            case "movie" -> ContentType.MOVIE;
            case "tvSeries" -> ContentType.TV;
            case "sport" -> ContentType.SPORT;
            default -> throw invalidValue(ErrorCode.CONTENT_TYPE_INVALID, "type", type);
        };
    }

    private SortDirection toSortDirection(String direction) {
        return switch (direction) {
            case "ASCENDING" -> SortDirection.ASCENDING;
            case "DESCENDING" -> SortDirection.DESCENDING;
            default -> throw invalidValue(ErrorCode.CONTENT_SORT_DIRECTION_INVALID, "sortDirection", direction);
        };
    }

    private BaseException invalidValue(ErrorCode errorCode, String field, Object value) {
        BaseException exception = new BaseException(errorCode);
        exception.addDetail(field, value);
        return exception;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record ResolvedQuery(
            ContentListCondition condition,
            String sortBy,
            String sortDirection
    ) {
    }
}
