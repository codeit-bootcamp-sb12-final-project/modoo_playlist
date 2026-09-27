package com.codeit.modoo_playlist.moduleapi.security.oauth.withdrawal;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class OAuthWithdrawalRequestStore {

  public static final String WITHDRAWAL_STATE_PREFIX = "withdrawal.";
  private static final String REQUEST_KEY_PREFIX = "auth:withdrawal:request:";
  private static final String STATE_KEY_PREFIX = "auth:withdrawal:state:";

  private final StringRedisTemplate redisTemplate;
  private final JsonMapper objectMapper;

  @Value("${module-api.auth.oauth2.withdrawal.request-expiration:5m}")
  private Duration expiration;

  public String create(OAuthWithdrawalRequest request) {
    String requestId = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(
        requestKey(requestId),
        objectMapper.writeValueAsString(request),
        expiration
    );
    return requestId;
  }

  public void bindState(String requestId, String state, String registrationId) {
    if (requestId == null || requestId.isBlank() || state == null || state.isBlank()) {
      throw new BaseException(ErrorCode.WITHDRAWAL_REQUEST_EXPIRED);
    }

    String value = redisTemplate.opsForValue().get(requestKey(requestId));
    if (value == null) {
      throw new BaseException(ErrorCode.WITHDRAWAL_REQUEST_EXPIRED);
    }

    OAuthWithdrawalRequest request = objectMapper.readValue(value, OAuthWithdrawalRequest.class);
    Provider requestedProvider = providerFromRegistrationId(registrationId);
    if (request.provider() != requestedProvider) {
      throw new BaseException(ErrorCode.INVALID_REQUEST);
    }

    redisTemplate.opsForValue().set(stateKey(state), requestId, expiration);
  }

  public Optional<OAuthWithdrawalRequest> consumeByState(String state) {
    if (state == null || state.isBlank()) {
      return Optional.empty();
    }

    String requestId = redisTemplate.opsForValue().getAndDelete(stateKey(state));
    if (requestId == null) {
      return Optional.empty();
    }

    String value = redisTemplate.opsForValue().getAndDelete(requestKey(requestId));
    if (value == null) {
      return Optional.empty();
    }

    return Optional.of(objectMapper.readValue(value, OAuthWithdrawalRequest.class));
  }

  public boolean isWithdrawalState(String state) {
    return state != null && state.startsWith(WITHDRAWAL_STATE_PREFIX);
  }

  private Provider providerFromRegistrationId(String registrationId) {
    try {
      return Provider.valueOf(registrationId.toUpperCase(Locale.ROOT));
    } catch (RuntimeException exception) {
      throw new BaseException(ErrorCode.INVALID_REQUEST, exception);
    }
  }

  private String requestKey(String requestId) {
    return REQUEST_KEY_PREFIX + requestId;
  }

  private String stateKey(String state) {
    return STATE_KEY_PREFIX + state;
  }
}
