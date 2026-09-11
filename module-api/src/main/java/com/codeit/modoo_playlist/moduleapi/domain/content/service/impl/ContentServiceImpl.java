package com.codeit.modoo_playlist.moduleapi.domain.content.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentPerson;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentSports;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTag;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentTagId;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentVideo;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;
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
import com.codeit.modoo_playlist.moduleapi.domain.content.storage.ThumbnailStorage;
import com.codeit.modoo_playlist.moduleapi.domain.tag.service.TagService;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentDetailResponse;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentListItemResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentServiceImpl implements ContentService {

    private static final int DEFAULT_LIMIT = 20;
    private static final String DEFAULT_SORT_BY = "watcherCount";
    private static final String DEFAULT_SORT_DIRECTION = "DESCENDING";

    private final ContentRepository contentRepository;
    private final ContentTagRepository contentTagRepository;
    private final ContentVideoRepository contentVideoRepository;
    private final ContentSportsRepository contentSportsRepository;
    private final ContentPersonRepository contentPersonRepository;
    private final TagService tagService;
    private final ContentMapper contentMapper;
    private final ThumbnailStorage thumbnailStorage;

    @Override
    public ContentCursorResponse getContents(ContentListRequest request) {
        ResolvedQuery resolvedQuery = resolveQuery(request);
        ContentQueryPage page = contentRepository.findAllByCondition(resolvedQuery.condition());
        Map<UUID, List<String>> tagsByContentId = loadTagsByContentId(
                page.contents().stream()
                        .map(ContentItem::content)
                        .map(Content::getId)
                        .toList()
        );

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
    public ContentDetailResponse getContent(UUID contentId) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BaseException(ErrorCode.CONTENT_NOT_FOUND));

        return createDetailResponse(content);
    }

    @Override
    @Transactional
    public ContentDetailResponse createContent(
            ContentCreateRequest request,
            MultipartFile thumbnail
    ) {
        Content content = Content.builder()
                .type(toContentType(request.type()))
                .title(request.title())
                .description(request.description())
                .thumbnailUrl(storeThumbnailIfPresent(thumbnail))
                .build();
        contentRepository.save(content);
        syncContentTags(content, request.tags());

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

        content.update(
                request.title(),
                request.description(),
                storeThumbnailIfPresent(thumbnail)
        );
        if (request.tags() != null) {
            syncContentTags(content, request.tags());
        }

        return createDetailResponse(content);
    }

    @Override
    @Transactional
    public void deleteContent(UUID contentId) {
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId)
                .orElseThrow(() -> new BaseException(ErrorCode.CONTENT_NOT_FOUND));
        content.softDelete();
    }

    private ContentDetailResponse createDetailResponse(Content content) {
        UUID contentId = content.getId();
        List<String> tags = loadTagsByContentId(List.of(contentId))
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
                people
        );
    }

    private void syncContentTags(Content content, List<String> requestedTags) {
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
    }

    private String storeThumbnailIfPresent(MultipartFile thumbnail) {
        if (thumbnail == null || thumbnail.isEmpty()) {
            return null;
        }
        try {
            return thumbnailStorage.store(thumbnail);
        } catch (IOException exception) {
            throw new BaseException(ErrorCode.FILE_SAVE_FAILED, exception);
        }
    }

    private Map<UUID, List<String>> loadTagsByContentId(List<UUID> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }

        return contentTagRepository.findAllWithTagByContentIds(contentIds).stream()
                .collect(Collectors.groupingBy(
                        contentTag -> contentTag.getId().getContentId(),
                        Collectors.mapping(
                                contentTag -> contentTag.getTag().getName(),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        names -> names.stream().sorted().toList()
                                )
                        )
                ));
    }

    private ResolvedQuery resolveQuery(ContentListRequest request) {
        String requestedSortBy = defaultIfBlank(request.sortBy(), DEFAULT_SORT_BY);
        String sortDirection = defaultIfBlank(request.sortDirection(), DEFAULT_SORT_DIRECTION);

        SortType sortType = switch (requestedSortBy) {
            case "watcherCount" -> SortType.WATCHER_COUNT;
            case "createdAt" -> SortType.CREATED_AT;
            case "rate", "averageRating" -> SortType.AVERAGE_RATING;
            case "recommended" -> SortType.WATCHER_COUNT;
            default -> throw invalidValue(ErrorCode.CONTENT_SORT_INVALID, "sortBy", requestedSortBy);
        };

        String effectiveSortBy = "recommended".equals(requestedSortBy)
                ? DEFAULT_SORT_BY
                : requestedSortBy;

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

        return new ResolvedQuery(condition, effectiveSortBy, sortDirection);
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
