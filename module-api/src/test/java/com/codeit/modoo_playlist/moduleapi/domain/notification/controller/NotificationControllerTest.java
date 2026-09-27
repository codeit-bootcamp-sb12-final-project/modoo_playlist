package com.codeit.modoo_playlist.moduleapi.domain.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.modoo_playlist.moduleapi.domain.notification.mapper.NotificationMapper;
import com.codeit.modoo_playlist.moduleapi.domain.notification.repository.query.NotificationQueryPage;
import com.codeit.modoo_playlist.moduleapi.domain.notification.service.NotificationService;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class NotificationControllerTest {
  @Test
  void notificationRoutesMatchClientUrls() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();
    UserDetails user = mock(UserDetails.class, RETURNS_DEEP_STUBS);
    when(user.getUserDto().id()).thenReturn(userId);
    NotificationService service = mock(NotificationService.class);
    when(service.getNotifications(any())).thenReturn(
        new NotificationQueryPage(List.of(), null, null, false, 0));
    var mvc = MockMvcBuilders.standaloneSetup(
        new NotificationController(service, mock(NotificationMapper.class)))
        .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType() == UserDetails.class;
          }
          @Override
          public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
              NativeWebRequest request, WebDataBinderFactory binderFactory) {
            return user;
          }
        }).build();
    mvc.perform(get("/api/notifications").param("limit", "20")
        .param("sortBy", "createdAt").param("sortDirection", "DESCENDING"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/notifications/unread-count")).andExpect(status().isOk());
    mvc.perform(patch("/api/notifications/{id}/read", notificationId))
        .andExpect(status().isNoContent());
    mvc.perform(patch("/api/notifications/read-all")).andExpect(status().isNoContent());
    verify(service).countUnread(userId);
    verify(service).readNotification(notificationId, userId);
    verify(service).readAllNotifications(userId);
  }
}
