package com.codeit.modoo_playlist.moduleapi.domain.chat.controller;

import com.codeit.modoo_playlist.core.global.common.dto.base.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.request.ChatMessageRequest;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ChatConversationCursorResponse;
import com.codeit.modoo_playlist.moduleapi.domain.chat.service.ChatService;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;

  @PreAuthorize("hasRole('USER')")
  @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<Object>> stream(@Valid @RequestBody ChatMessageRequest request,
      @AuthenticationPrincipal UserDetails user
  ) {
    return chatService.chat(user.getUserDto().id(), request.conversationId(), request.message());
  }

  @PreAuthorize("hasRole('USER')")
  @GetMapping("conversations")
  public ResponseEntity<ChatConversationCursorResponse> getConversations(
      @Valid @ModelAttribute SliceCursorRequest request, @AuthenticationPrincipal UserDetails user){
    return ResponseEntity.ok(chatService.getConversations(user.getUserDto().id(), request));
  }
}
