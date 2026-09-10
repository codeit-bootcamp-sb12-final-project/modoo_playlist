package com.codeit.modoo_playlist.moduleapi.discovery.chat.service;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

public interface ChatService {
	Flux<ServerSentEvent<Object>> chat(UUID userId, UUID conversationId, @NotBlank String message);

	Flux<ServerSentEvent<Object>> chatAnonymous(UUID uuid, @NotBlank String message);
}
