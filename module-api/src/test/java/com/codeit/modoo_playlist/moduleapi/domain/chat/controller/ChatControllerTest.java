package com.codeit.modoo_playlist.moduleapi.domain.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.request.ChatMessageRequest;
import com.codeit.modoo_playlist.moduleapi.domain.chat.service.ChatService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

  @Mock private ChatService chatService;
  @InjectMocks private ChatController controller;

  @Test
  void 로그인_사용자면_chat으로_스트림을_시작한다() {
    UUID userId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UserDetails user = new UserDetails(
        new UserDto(userId, "user@test.com", "user", null, UserRole.USER, false, null), "password");
    Flux<ServerSentEvent<Object>> expected = Flux.empty();
    when(chatService.chat(userId, conversationId, "안녕")).thenReturn(expected);

    Flux<ServerSentEvent<Object>> result =
        controller.stream(new ChatMessageRequest(conversationId, "안녕"), user);

    assertThat(result).isSameAs(expected);
    verify(chatService).chat(userId, conversationId, "안녕");
    verifyNoMoreInteractions(chatService);
  }
}
