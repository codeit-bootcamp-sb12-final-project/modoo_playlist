package com.codeit.modoo_playlist.moduleapi.domain.conversation.controller;

import com.codeit.modoo_playlist.moduleapi.domain.conversation.service.ConversationService;
import com.codeit.modoo_playlist.moduleapi.dto.ConversationDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.ConversationCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.request.SliceCursorRequest;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.response.CursorResponseConversationDto;
import com.codeit.modoo_playlist.moduleapi.dto.conversation.response.CursorResponseMessageDto;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    // 대화 생성
    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ConversationCreateRequest request
    ) {
        UUID requesterId = userDetails.getUserDto().id();
        ConversationDto conversation =
                conversationService.create(
                        requesterId,
                        request
                );
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    // 대화 목록 조회
    @GetMapping()
    public ResponseEntity<CursorResponseConversationDto> findConversations (
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute SliceCursorRequest request
    ){
        CursorResponseConversationDto response =
                conversationService.findConversations(
                        userDetails.getUserDto().id(),
                        request
                );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // 대화 조회
    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationDto> findConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID conversationId
    ){
        UUID requesterId = userDetails.getUserDto().id();
        ConversationDto conversation = conversationService.findConversation(requesterId, conversationId);
        return ResponseEntity.status(HttpStatus.OK).body(conversation);
    }

    // 특정 사용자와의 대화 조회
    @GetMapping("/with")
    public ResponseEntity<ConversationDto> findConversationWithUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam UUID userId
    ){
        UUID requesterId = userDetails.getUserDto().id();

        ConversationDto conversation =
                conversationService.findByReceiverId(requesterId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(conversation);
    }

    // DM 목록 조회
    @GetMapping("/{conversationId}/direct-messages")
    public ResponseEntity<CursorResponseMessageDto> findDirectMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID conversationId,
            @ModelAttribute SliceCursorRequest request
    ){
        UUID requesterId = userDetails.getUserDto().id();
        CursorResponseMessageDto response =
                conversationService.findMessages(
                        requesterId,
                        conversationId,
                        request
                );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // DM 읽음 처리
    @PostMapping("/{conversationId}/direct-messages/{messageId}/read")
    public ResponseEntity<Void> readDirectMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId
    ){
        conversationService.readDirectMessage(
                userDetails.getUserDto().id(),
                conversationId,
                messageId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }


}
