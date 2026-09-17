package com.codeit.modoo_playlist.moduleapi.mapper;

import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.moduleapi.domain.content.mapper.ContentMapper;
import com.codeit.modoo_playlist.moduleapi.dto.WatchingSessionDto;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class, ContentMapper.class})
public interface WatchingSessionMapper {

    @Mapping(target = "id", source = "session.id")
    @Mapping(target = "createdAt", source = "session.createdAt")
    @Mapping(target = "watcher", source = "session.watcher")
    @Mapping(target = "content", source = "contentSummary")
    WatchingSessionDto toDto(WatchingSession session, ContentSummaryResponse contentSummary);

}
