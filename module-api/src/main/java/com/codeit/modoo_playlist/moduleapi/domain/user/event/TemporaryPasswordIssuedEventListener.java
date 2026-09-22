package com.codeit.modoo_playlist.moduleapi.domain.user.event;

import com.codeit.modoo_playlist.moduleapi.domain.user.service.TemporaryPasswordSender;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemporaryPasswordIssuedEventListener {

  private final LoginSessionStore loginSessionStore;
  private final TemporaryPasswordSender temporaryPasswordSender;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(TemporaryPasswordIssuedEvent event) {
    try {
      loginSessionStore.invalidateAll(event.userId());
    } catch (RuntimeException exception) {
      log.error(
          "Failed to invalidate login sessions after password reset: userId={}",
          event.userId(),
          exception
      );
      return;
    }

    try {
      temporaryPasswordSender.send(
          event.email(),
          event.temporaryPassword(),
          event.expiresAt()
      );
    } catch (RuntimeException exception) {
      log.error(
          "Failed to send temporary password after password reset: userId={}",
          event.userId(),
          exception
      );
    }
  }
}
