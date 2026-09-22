package com.codeit.modoo_playlist.infra.mapper;

import com.codeit.modoo_playlist.core.domain.content.dto.ContentSummaryResponse;
import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.content.type.ContentType;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ContentSummaryMapper {

    ContentSummaryResponse toSummary(Content content, List<String> tags);

    default String toApiType(ContentType type) {

        if(type == null) return null;

        return switch (type) {
            case MOVIE -> "movie";
            case TV -> "tvSeries";
            case SPORT -> "sport";
        };
    }

}
