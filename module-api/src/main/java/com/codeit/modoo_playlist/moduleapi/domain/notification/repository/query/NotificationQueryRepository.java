package com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query;

public interface NotificationQueryRepository {

    NotificationQueryPage findAllByCondition(NotificationListCondition condition);

}
