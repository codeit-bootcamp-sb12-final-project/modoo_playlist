package com.codeit.modoo_playlist.modulerealtime.handler;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.core.global.exception.ErrorResponse;
import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.modulerealtime.dto.chat.ContentChatSendRequest;
import com.codeit.modoo_playlist.modulerealtime.message.controller.MessageController;
import com.codeit.modoo_playlist.modulerealtime.message.service.MessageService;
import com.codeit.modoo_playlist.modulerealtime.security.RealtimePrincipal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.socket.messaging.WebSocketAnnotationMethodMessageHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebSocketExceptionHandlerTest {
    @Test
    void businessAndValidationErrorsAreReturnedToRequestingSession() {
        MessageService service = mock(MessageService.class);
        BaseException failure = new BaseException(ErrorCode.ACCESS_DENIED);
        failure.addDetail("reason", "not allowed");
        when(service.sendContentChat(any(), any(), any())).thenThrow(failure);

        try (var context = new AnnotationConfigApplicationContext();
             var validator = new LocalValidatorFactoryBean()) {
            context.registerBean(MessageController.class,
                    () -> new MessageController(service, mock(RealtimeNotifier.class)));
            context.registerBean(WebSocketExceptionHandler.class);
            context.refresh();
            validator.afterPropertiesSet();

            var inbound = new ExecutorSubscribableChannel();
            var outbound = new ExecutorSubscribableChannel();
            var broker = new ExecutorSubscribableChannel();
            List<Message<?>> replies = new ArrayList<>();
            broker.subscribe(replies::add);
            var handler = new WebSocketAnnotationMethodMessageHandler(
                    inbound, outbound, new SimpMessagingTemplate(broker));
            handler.setApplicationContext(context);
            handler.setDestinationPrefixes(List.of("/pub"));
            handler.setValidator(validator);
            handler.afterPropertiesSet();

            handler.handleMessage(request("hello"));
            handler.handleMessage(request(" "));

            assertThat(replies).hasSize(2);
            for (Message<?> reply : replies) {
                assertThat(SimpMessageHeaderAccessor.getDestination(reply.getHeaders()))
                        .isEqualTo("/user/user@example.com/sub/errors");
                assertThat(SimpMessageHeaderAccessor.getSessionId(reply.getHeaders()))
                        .isEqualTo("request-session");
            }
            ErrorResponse business = (ErrorResponse) replies.get(0).getPayload();
            assertThat(business.getCode()).isEqualTo("ACCESS_DENIED");
            assertThat(business.getStatus()).isEqualTo(403);
            assertThat(business.getDetails()).containsEntry("reason", "not allowed");
            ErrorResponse validation = (ErrorResponse) replies.get(1).getPayload();
            assertThat(validation.getCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(validation.getStatus()).isEqualTo(400);
            assertThat(validation.getDetails()).containsKey("content");
            verify(service, times(1)).sendContentChat(any(), any(), any());
        }
    }

    private Message<?> request(String content) {
        var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setDestination("/pub/contents/" + UUID.randomUUID() + "/chat");
        headers.setSessionId("request-session");
        headers.setSessionAttributes(new HashMap<>());
        headers.setUser(UsernamePasswordAuthenticationToken.authenticated(
                new RealtimePrincipal(UUID.randomUUID(), "user@example.com"), null, List.of()));
        return MessageBuilder.createMessage(new ContentChatSendRequest(content), headers.getMessageHeaders());
    }
}
