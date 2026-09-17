package com.codeit.modoo_playlist.moduleapi.security;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.mapper.UserMapper;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserAuthenticationProvider implements AuthenticationProvider {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  //  AuthenticationProvider에서 로그인 검증을 할때 Credential을 추가하기 위해서.
  @Override
  @Transactional(readOnly = true)
  public Authentication authenticate(Authentication authentication)
      throws AuthenticationException {
    String email = authentication.getName();
    String rawPassword = String.valueOf(authentication.getCredentials());

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BadCredentialsException(
            ErrorCode.INVALID_CREDENTIALS.getMessage()
        ));

    Instant now = clock.instant();
    boolean temporaryMatches = user.getTempPassword() != null
        && passwordEncoder.matches(rawPassword, user.getTempPassword());
    boolean permanentMatches = user.getPassword() != null
        && passwordEncoder.matches(rawPassword, user.getPassword());

    if (!temporaryMatches && !permanentMatches) {
      throw invalidCredentials();
    }

    if (user.isLocked()) {
      throw new LockedException(ErrorCode.USER_ACCOUNT_LOCKED.getMessage());
    }

    LoginCredentialType credentialType;

    if (user.hasActiveTemporaryPassword(now)) {
      if (temporaryMatches) {
        credentialType = LoginCredentialType.TEMPORARY;
      } else if (permanentMatches) {
        throw new CodedAuthenticationException(ErrorCode.TEMP_PASSWORD_ACTIVE);
      } else {
        throw invalidCredentials();
      }
    } else {
      if (temporaryMatches) {
        throw new CodedAuthenticationException(ErrorCode.TEMP_PASSWORD_EXPIRED);
      }

      if (!permanentMatches) {
        throw invalidCredentials();
      }

      credentialType = LoginCredentialType.PERMANENT;
    }

    UserDetails principal = new UserDetails(
        userMapper.toDto(user),
        credentialType == LoginCredentialType.TEMPORARY
            ? user.getTempPassword()
            : user.getPassword(),
        credentialType
    );

    UsernamePasswordAuthenticationToken result =
        UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities()
        );
    result.setDetails(authentication.getDetails());
    return result;
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  private BadCredentialsException invalidCredentials() {
    return new BadCredentialsException(ErrorCode.INVALID_CREDENTIALS.getMessage());
  }
}
