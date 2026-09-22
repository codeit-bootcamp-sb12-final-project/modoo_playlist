package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.core.domain.user.entity.SocialAccount;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.SocialAccountRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequest;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthWithdrawalTransactionService {

  private final UserRepository userRepository;
  private final SocialAccountRepository socialAccountRepository;

  @Transactional
  public void withdraw(OAuthWithdrawalRequest request) {
    User user = userRepository.findByIdForUpdate(request.userId())
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    if (user.getDeletedAt() != null) {
      throw new BaseException(ErrorCode.USER_ACCOUNT_WITHDRAWN);
    }

    SocialAccount socialAccount = socialAccountRepository.findByUserId(request.userId())
        .orElseThrow(() -> new BaseException(ErrorCode.OAUTH_ACCOUNT_MISMATCH));
    if (socialAccount.getProvider() != request.provider()
        || !Objects.equals(socialAccount.getProviderUserId(), request.providerUserId())) {
      throw new BaseException(ErrorCode.OAUTH_ACCOUNT_MISMATCH);
    }

    user.withdraw(Instant.now());
  }
}
