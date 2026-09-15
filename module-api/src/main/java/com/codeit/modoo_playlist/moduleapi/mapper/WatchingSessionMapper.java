package com.codeit.modoo_playlist.moduleapi.mapper;

import com.codeit.modoo_playlist.core.domain.watchingSession.entity.WatchingSession;
import com.codeit.modoo_playlist.moduleapi.domain.content.mapper.ContentMapper;
import com.codeit.modoo_playlist.moduleapi.dto.WatchingSessionDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",
uses = {UserMapper.class, ContentMapper.class})
public interface WatchingSessionMapper {

    WatchingSessionDto toDto(WatchingSession watchingSession);
}
