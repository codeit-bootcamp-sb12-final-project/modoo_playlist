package com.codeit.modoo_playlist.moduleapi.domain.user.service;

import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginIssueResult;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.TokenRefreshResult;
import java.util.UUID;

public interface AuthService {

  void resetPassword(String email);

  LoginIssueResult issueLogin(UUID userId);

  TokenRefreshResult refresh(String refreshToken);

}
