package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.domain.user.entity.SocialAccount;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.SocialAccountRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.OAuthWithdrawalService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.SocialAccountUnlinkClient;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.OAuthWithdrawalAuthorizationResponse;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequest;
import com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal.OAuthWithdrawalRequestStore;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthWithdrawalServiceImpl implements OAuthWithdrawalService {

  private final UserRepository userRepository;
  private final SocialAccountRepository socialAccountRepository;
  private final OAuthWithdrawalRequestStore requestStore;
  private final SocialAccountUnlinkClient unlinkClient;
  private final LoginSessionStore loginSessionStore;

  @Override
  @Transactional(readOnly = true)
  public OAuthWithdrawalAuthorizationResponse prepare(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    if (user.getDeletedAt() != null) {
      throw new BaseException(ErrorCode.USER_ACCOUNT_WITHDRAWN);
    }

    SocialAccount socialAccount = socialAccountRepository.findByUserId(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.INVALID_REQUEST));
    if (socialAccount.getProvider() == Provider.LOCAL) {
      throw new BaseException(ErrorCode.INVALID_REQUEST);
    }

    String requestId = requestStore.create(new OAuthWithdrawalRequest(
        userId,
        socialAccount.getProvider(),
        socialAccount.getProviderUserId()
    ));
    String registrationId = socialAccount.getProvider().name().toLowerCase(Locale.ROOT);
    return new OAuthWithdrawalAuthorizationResponse(
        "/oauth2/authorization/" + registrationId + "?withdrawalRequestId=" + requestId
    );
  }

  @Override
  @Transactional
  public void complete(
      OAuthWithdrawalRequest request,
      OAuthUserProfile authenticatedProfile,
      String providerAccessToken
  ) {
    if (request.provider() != authenticatedProfile.provider()
        || !Objects.equals(request.providerUserId(), authenticatedProfile.providerUserId())) {
      throw new BaseException(ErrorCode.OAUTH_ACCOUNT_MISMATCH);
    }

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

    unlinkClient.unlink(request.provider(), providerAccessToken);
    user.withdraw(Instant.now());
    loginSessionStore.invalidateAll(request.userId());
  }
}
