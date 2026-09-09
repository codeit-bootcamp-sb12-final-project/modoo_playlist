package com.codeit.modoo_playlist.moduleapi.domain.content.service.impl;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentPerson;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentSports;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentVideo;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
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
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
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
    private final ContentMapper contentMapper;

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
                .orElseThrow(() -> new NoSuchElementException("콘텐츠를 찾을 수 없습니다."));

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
            default -> throw new IllegalArgumentException("지원하지 않는 sortBy 값입니다.");
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
            default -> throw new IllegalArgumentException("지원하지 않는 콘텐츠 타입입니다.");
        };
    }

    private SortDirection toSortDirection(String direction) {
        return switch (direction) {
            case "ASCENDING" -> SortDirection.ASCENDING;
            case "DESCENDING" -> SortDirection.DESCENDING;
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 방향입니다.");
        };
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
