package com.codeit.modoo_playlist.moduleapi.domain.chat.controller;

import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.requrst.ChatMessageRequest;
import com.codeit.modoo_playlist.moduleapi.domain.chat.service.ChatService;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/discovery/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<Object>> stream(@Valid @RequestBody ChatMessageRequest request,
	                                            @AuthenticationPrincipal UserDetails user
	) {
		if (user != null) {
			return chatService.chat(user.getUserDto().id(), request.conversationId(), request.message());
		}
		return chatService.chatAnonymous(request.conversationId(), request.message());
	}

}
