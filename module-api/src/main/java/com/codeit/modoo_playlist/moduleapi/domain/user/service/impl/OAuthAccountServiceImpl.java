package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.core.domain.user.entity.SocialAccount;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.SocialAccountRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.OAuthAccountService;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthAccountResult;
import com.codeit.modoo_playlist.moduleapi.dto.oauth.OAuthUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthAccountServiceImpl implements OAuthAccountService {

  private final SocialAccountRepository socialAccountRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public OAuthAccountResult resolveOrCreate(OAuthUserProfile profile) {
    return socialAccountRepository.findByProviderAndProviderUserId(
            profile.provider(),
            profile.providerUserId()
        )
        .map(socialAccount -> new OAuthAccountResult(
            socialAccount.getUser().getId(),
            false
        ))
        .orElseGet(() -> createAccount(profile));
  }

  private OAuthAccountResult createAccount(OAuthUserProfile profile) {
    if (userRepository.existsByEmail(profile.email())) {
      throw new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    User user = userRepository.save(User.createOAuth(
        profile.email(),
        profile.name(),
        profile.profileImageUrl()
    ));

    socialAccountRepository.save(SocialAccount.create(
        user,
        profile.provider(),
        profile.providerUserId()
    ));

    return new OAuthAccountResult(user.getId(), true);
  }
}
