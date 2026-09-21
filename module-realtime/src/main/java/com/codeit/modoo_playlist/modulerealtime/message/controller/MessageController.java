package com.codeit.modoo_playlist.modulerealtime.message.controller;

import com.codeit.modoo_playlist.core.domain.message.entity.MessageDto;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;

import com.codeit.modoo_playlist.modulerealtime.dto.chat.ContentChatDto;
import com.codeit.modoo_playlist.modulerealtime.dto.chat.ContentChatSendRequest;
import com.codeit.modoo_playlist.modulerealtime.dto.chat.DirectMessageSendRequest;
import com.codeit.modoo_playlist.modulerealtime.message.service.MessageService;
import com.codeit.modoo_playlist.modulerealtime.security.RealtimePrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final RealtimeNotifier realtimeNotifier;

    // 콘텐츠 실시간 채팅
    // 메시지 전송
    @MessageMapping("/contents/{contentId}/chat")
    public void sendContentChat(
            @DestinationVariable UUID contentId,
            @Payload @Valid ContentChatSendRequest payload,
            Principal principal
    ){
        ContentChatDto response = messageService.sendContentChat(
                extractSenderId(principal), contentId, payload
        );

        realtimeNotifier.notifyStomp(
                "/sub/contents/" + contentId + "/chat",
                response
        );
    }

    // DM
    // 메시지 전송
    @MessageMapping("/conversations/{conversationId}/direct-messages")
    public void sendDirectMessage(
            @DestinationVariable UUID conversationId,
            @Payload @Valid DirectMessageSendRequest payload,
            Principal principal
    ) {
        UUID senderId = extractSenderId(principal);

        MessageDto saved =
                messageService.sendDirectMessage(
                        senderId,
                        conversationId,
                        payload
                );

        realtimeNotifier.notifyStomp(
            "/sub/conversations/" + conversationId + "/direct-messages", saved);
    }

    private UUID extractSenderId(Principal principal) {
        if(!(principal instanceof Authentication auth)
        || !auth.isAuthenticated()
            || !(auth.getPrincipal() instanceof RealtimePrincipal user)){
            throw new BaseException(ErrorCode.ACCESS_DENIED);
        }
        return user.userId();
    }
}
