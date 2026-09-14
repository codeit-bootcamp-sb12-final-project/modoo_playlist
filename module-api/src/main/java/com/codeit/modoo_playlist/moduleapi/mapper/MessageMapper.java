package com.codeit.modoo_playlist.moduleapi.mapper;

import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.moduleapi.dto.MessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = UserMapper.class)
public interface MessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "content", source = "message")
    MessageDto toDto(Message message);
}
