package com.codeit.modoo_playlist.moduleapi.domain.content.mapper;

import java.util.List;

import com.codeit.modoo_playlist.moduleapi.dto.content.response.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentPerson;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentSports;
import com.codeit.modoo_playlist.core.domain.content.entity.ContentVideo;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.query.ContentQueryPage;

@Mapper(componentModel = "spring")
public interface ContentMapper {

    ContentListItemResponse toListItem(
            Content content,
            List<String> tags,
            long watcherCount
    );

    ContentSummaryResponse toSummary(Content content, List<String> tags);

    ContentDetailResponse toDetail(
            Content content,
            List<String> tags,
            long watcherCount,
            ContentVideo video,
            ContentSports sports,
            List<ContentPerson> people
    );

    @Mapping(target = "nextCursor", source = "page.nextCursor")
    @Mapping(target = "nextIdAfter", source = "page.nextIdAfter")
    @Mapping(target = "hasNext", source = "page.hasNext")
    @Mapping(target = "totalCount", source = "page.totalCount")
    ContentCursorResponse toCursorResponse(
            ContentQueryPage page,
            List<ContentListItemResponse> data,
            String sortBy,
            String sortDirection
    );

    ContentVideoResponse toVideoResponse(ContentVideo video);

    ContentSportsResponse toSportsResponse(ContentSports sports);

    ContentPersonResponse toPersonResponse(ContentPerson person);

    default String toApiType(ContentType type) {
        return switch (type) {
            case MOVIE -> "movie";
            case TV -> "tvSeries";
            case SPORT -> "sport";
        };
    }
}
