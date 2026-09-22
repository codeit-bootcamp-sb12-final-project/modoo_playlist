package com.codeit.modoo_playlist.moduleapi.domain.user.repository;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.domain.user.entity.SocialAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

  Optional<SocialAccount> findByProviderAndProviderUserId(
      Provider provider,
      String providerUserId
  );

  Optional<SocialAccount> findByUserId(UUID userId);
}
