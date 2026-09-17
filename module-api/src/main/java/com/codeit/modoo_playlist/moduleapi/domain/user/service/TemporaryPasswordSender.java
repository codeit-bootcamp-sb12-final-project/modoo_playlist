package com.codeit.modoo_playlist.moduleapi.domain.user.service;

import java.time.Instant;

public interface TemporaryPasswordSender {

  void send(String email, String temporaryPassword, Instant expiresAt);
}
