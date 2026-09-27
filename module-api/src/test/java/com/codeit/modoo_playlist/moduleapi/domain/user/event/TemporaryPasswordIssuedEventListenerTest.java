package com.codeit.modoo_playlist.moduleapi.domain.user.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import com.codeit.modoo_playlist.moduleapi.domain.user.service.TemporaryPasswordSender;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemporaryPasswordIssuedEventListenerTest {

  @Mock
  LoginSessionStore loginSessionStore;

  @Mock
  TemporaryPasswordSender temporaryPasswordSender;

  private TemporaryPasswordIssuedEventListener listener;
  private TemporaryPasswordIssuedEvent event;

  @BeforeEach
  void setUp() {
    listener = new TemporaryPasswordIssuedEventListener(
        loginSessionStore,
        temporaryPasswordSender
    );
    event = new TemporaryPasswordIssuedEvent(
        UUID.randomUUID(),
        "user@example.com",
        "temporary1!!",
        Instant.parse("2026-01-01T00:03:00Z")
    );
  }

  @Test
  void invalidatesSessionsBeforeSendingEmail() {
    listener.handle(event);

    InOrder order = inOrder(loginSessionStore, temporaryPasswordSender);
    order.verify(loginSessionStore).invalidateAll(event.userId());
    order.verify(temporaryPasswordSender)
        .send(event.email(), event.temporaryPassword(), event.expiresAt());
  }

  @Test
  void doesNotSendEmailWhenSessionInvalidationFails() {
    doThrow(new IllegalStateException("redis unavailable"))
        .when(loginSessionStore)
        .invalidateAll(event.userId());

    assertThatCode(() -> listener.handle(event)).doesNotThrowAnyException();

    verifyNoInteractions(temporaryPasswordSender);
  }

  @Test
  void doesNotPropagateEmailFailureAfterCommit() {
    doThrow(new IllegalStateException("smtp unavailable"))
        .when(temporaryPasswordSender)
        .send(event.email(), event.temporaryPassword(), event.expiresAt());

    assertThatCode(() -> listener.handle(event)).doesNotThrowAnyException();
  }
}
