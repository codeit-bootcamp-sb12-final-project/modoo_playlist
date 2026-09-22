package com.codeit.modoo_playlist.infra.mapper;

import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.infra.mapper.ContentSummaryMapper;
import com.codeit.modoo_playlist.core.domain.watchingSession.dto.WatchingSessionDto;
import com.codeit.modoo_playlist.core.domain.content.dto.ContentSummaryResponse;
import com.codeit.modoo_playlist.infra.mapper.UserSummaryMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {UserSummaryMapper.class, ContentSummaryMapper.class})
public interface WatchingSessionMapper {

    @Mapping(target = "id", source = "session.id")
    @Mapping(target = "createdAt", source = "session.createdAt")
    @Mapping(target = "watcher", source = "session.watcher")
    @Mapping(target = "content", source = "contentSummary")
    WatchingSessionDto toDto(WatchingSession session, ContentSummaryResponse contentSummary);

}
