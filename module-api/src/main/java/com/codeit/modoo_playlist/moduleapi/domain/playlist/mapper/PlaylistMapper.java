package com.codeit.modoo_playlist.moduleapi.domain.playlist.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.codeit.modoo_playlist.core.domain.playlist.entity.Playlist;
import com.codeit.modoo_playlist.moduleapi.domain.playlist.repository.query.PlaylistQueryPage;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentSummaryResponse;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.response.PlaylistCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.playlist.response.PlaylistResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.UserSummaryResponse;

@Mapper(componentModel = "spring")
public interface PlaylistMapper {

    PlaylistResponse toResponse(Playlist playlist, UserSummaryResponse owner,
                                long subscriberCount, boolean subscribedByMe, List<ContentSummaryResponse> contents);

    @Mapping(target = "nextCursor", source = "page.nextCursor")
    @Mapping(target = "nextIdAfter", source = "page.nextIdAfter")
    @Mapping(target = "hasNext", source = "page.hasNext")
    @Mapping(target = "totalCount", source = "page.totalCount")
    PlaylistCursorResponse toCursorResponse(
            PlaylistQueryPage page,
            List<PlaylistResponse> data,
            String sortBy,
            String sortDirection
    );
}