package com.codeit.modoo_playlist.moduleapi.domain.notification.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.modoo_playlist.core.domain.notification.entity.Notification;
import com.codeit.modoo_playlist.core.domain.notification.entity.NotificationLevel;
import com.codeit.modoo_playlist.moduleapi.dto.notification.response.NotificationResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationMapperTest {

    private final NotificationMapper mapper = new NotificationMapperImpl();

    @Test
    void 읽지_않은_알림은_isRead가_false로_매핑된다() {
        Notification notification = buildNotification();

        NotificationResponse response = mapper.toResponse(notification);

        assertThat(response.isRead()).isFalse();
    }

    @Test
    void markAsRead_호출_후에는_isRead가_true로_매핑된다() {
        Notification notification = buildNotification();
        notification.markAsRead();

        NotificationResponse response = mapper.toResponse(notification);

        assertThat(response.isRead()).isTrue();
    }

    @Test
    void receiverId_title_content_level_필드도_그대로_매핑된다() {
        Notification notification = buildNotification();

        NotificationResponse response = mapper.toResponse(notification);

        assertThat(response.receiverId()).isEqualTo(notification.getReceiverId());
        assertThat(response.title()).isEqualTo(notification.getTitle());
        assertThat(response.content()).isEqualTo(notification.getContent());
        assertThat(response.level()).isEqualTo(notification.getLevel());
    }

    private Notification buildNotification() {

        NotificationLevel anyLevel = NotificationLevel.values()[0];

        return Notification.builder()
                .receiverId(UUID.randomUUID())
                .title("title")
                .content("content")
                .level(anyLevel)
                .sourceId(UUID.randomUUID())
                .build();
    }
}