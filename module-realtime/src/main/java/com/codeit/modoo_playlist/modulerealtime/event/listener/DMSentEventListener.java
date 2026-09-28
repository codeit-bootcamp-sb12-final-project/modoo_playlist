package com.codeit.modoo_playlist.modulerealtime.event.listener;

import com.codeit.modoo_playlist.core.domain.notification.dto.NotificationResponse;
import com.codeit.modoo_playlist.core.domain.notification.entity.Notification;
import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.realtime.RealtimeNotifier;
import com.codeit.modoo_playlist.infra.event.DMSentEvent;
import com.codeit.modoo_playlist.infra.repository.RealtimeNotificationRepository;
import com.codeit.modoo_playlist.infra.repository.RealtimeUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DMSentEventListener {

    private static final String NOTIFICATION_ASYNC_EXECUTOR = "notificationAsyncExecutor";
    private final RealtimeNotifier realtimeNotifier;
    private final RealtimeUserRepository userRepository;
    private final PlatformTransactionManager transactionManager;
    private final RealtimeNotificationRepository notificationRepository;

    @Async(NOTIFICATION_ASYNC_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSendMessaged(DMSentEvent event) {
        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        NotificationResponse response = transaction.execute(status ->
                saveNotification(event));

        realtimeNotifier.notifySse(
                Set.of(response.receiverId()),
                "notifications",
                response
        );
    }

    NotificationResponse saveNotification(DMSentEvent event) {
        String senderName = userRepository.findById(event.ownerId())
                .map(User::getUsername)
                .orElse("알 수 없음");

        Notification saved = notificationRepository.save(
                Notification.builder()
                        .receiverId(event.receiverId())
                        .title("[DM] " + senderName)
                        .content(event.content())
                        .level(NotificationLevel.INFO)
                        .sourceId(event.messageId())
                        .build()
        );

        return new NotificationResponse(
                saved.getReceiverId(),
                saved.getCreatedAt(),
                saved.getReceiverId(),
                saved.getTitle(),
                saved.getContent(),
                saved.getLevel(),
                saved.isRead()
        );
    }
}
