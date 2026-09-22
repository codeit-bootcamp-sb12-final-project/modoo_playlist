package com.codeit.modoo_playlist.moduleapi.domain.chat.service;

import com.codeit.modoo_playlist.core.global.common.dto.base.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ChatConversationCursorResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface ChatService {
	Flux<ServerSentEvent<Object>> chat(UUID userId, UUID conversationId, @NotBlank String message);

	ChatConversationCursorResponse getConversations(UUID userId, SliceCursorRequest request);
}
