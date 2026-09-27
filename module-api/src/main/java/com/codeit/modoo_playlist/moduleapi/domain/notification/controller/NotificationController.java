package com.codeit.modoo_playlist.moduleapi.domain.notification.controller;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.notification.mapper.NotificationMapper;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationListCondition;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.notification.service.NotificationService;
import com.codeit.modoo_playlist.moduleapi.sse.SseEmitterRepository;
import com.codeit.modoo_playlist.moduleapi.dto.notification.response.NotificationCursorResponse;
import com.codeit.modoo_playlist.core.domain.notification.dto.NotificationResponse;
import com.codeit.modoo_playlist.moduleapi.dto.notification.response.NotificationUnreadCountResponse;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;

    @PreAuthorize("hasRole('USER')")
    @GetMapping
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
    @GetMapping("/unread-count")
    public ResponseEntity<NotificationUnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserDetails user
    ) {
        long count = notificationService.countUnread(user.getUserDto().id());
        return ResponseEntity.ok(new NotificationUnreadCountResponse(count));
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> readNotification(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal UserDetails user
    ) {
        notificationService.readNotification(notificationId, user.getUserDto().id());
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> readAllNotifications(
            @AuthenticationPrincipal UserDetails user
    ) {
        notificationService.readAllNotifications(user.getUserDto().id());
        return ResponseEntity.noContent().build();
    }

}
