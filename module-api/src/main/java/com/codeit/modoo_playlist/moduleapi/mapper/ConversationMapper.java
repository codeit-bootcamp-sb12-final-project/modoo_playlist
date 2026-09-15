package com.codeit.modoo_playlist.moduleapi.mapper;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.dto.ConversationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class, MessageMapper.class}
)
public interface ConversationMapper {

    @Mapping(target = "id", source = "conversation.id")
    @Mapping(target = "with", source = "withUser")
    @Mapping(target = "lastestMessage", source = "lastestMessage")
    @Mapping(target = "hasUnread", source = "hasUnread")
    ConversationDto toDto(
            Conversation conversation,
            User withUser,
            Message lastestMessage,
            boolean hasUnread
    );
}
