package com.codeit.modoo_playlist.moduleapi.mapper;

import com.codeit.modoo_playlist.core.domain.conversation.entity.Conversation;
import com.codeit.modoo_playlist.core.domain.message.entity.Message;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.dto.ConversationDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.response.CursorResponseConversationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Pageable;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    @Mapping(target = "id", source = "conversation.id")
    @Mapping(target = "with", source = "withUser")
    @Mapping(target = "latestMessage", source = "latestMessage")
    @Mapping(target = "hasUnread", source = "hasUnread")
    ConversationDto toDto(
            Conversation conversation,
            User withUser,
            Message latestMessage,
            boolean hasUnread
    );

    CursorResponseConversationDto toCursorDto(Conversation conversation, Pageable pageable);
}
