package com.codeit.modoo_playlist.moduleapi.domain.notification.controller;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.notification.mapper.NotificationMapper;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.notification.service.NotificationService;
import com.codeit.modoo_playlist.moduleapi.domain.notification.sse.SseEmitterRepository;
import com.codeit.modoo_playlist.moduleapi.dto.notification.response.NotificationCursorResponse;
import com.codeit.modoo_playlist.moduleapi.dto.notification.response.NotificationResponse;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final SseEmitterRepository sseEmitterRepository;

    @PreAuthorize("hasRole('USER')")
    @GetMapping(value = "/api/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails user) {
        return sseEmitterRepository.connect(user.getUserDto().id());
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/notifications")
    public ResponseEntity<NotificationCursorResponse> getNotifications(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID idAfter,
            @RequestParam int limit,
            @RequestParam String sortBy,
            @RequestParam NotificationListCondition.SortDirection sortDirection,
            @AuthenticationPrincipal UserDetails user
    ) {
        if (!"createdAt".equals(sortBy)) {
            throw new BaseException(ErrorCode.NOTIFICATION_QUERY_INVALID);
        }

        NotificationListCondition condition = new NotificationListCondition(
                user.getUserDto().id(), cursor, idAfter, limit, sortDirection
        );

        NotificationQueryPage page = notificationService.getNotifications(condition);
        List<NotificationResponse> data = page.notifications().stream()
                .map(notificationMapper::toResponse)
                .toList();

        NotificationCursorResponse response = new NotificationCursorResponse(
                data,
                page.nextCursor(),
                page.nextIdAfter(),
                page.hasNext(),
                page.totalCount(),
                sortBy,
                sortDirection.name()
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/api/notifications/{notificationId}")
    public ResponseEntity<Void> readNotification(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal UserDetails user
    ) {
        notificationService.readNotification(notificationId, user.getUserDto().id());
        return ResponseEntity.noContent().build();
    }

}
