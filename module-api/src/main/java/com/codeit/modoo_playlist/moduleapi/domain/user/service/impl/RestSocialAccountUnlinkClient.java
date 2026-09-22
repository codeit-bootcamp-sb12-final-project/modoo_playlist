package com.codeit.modoo_playlist.moduleapi.domain.user.service.impl;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.SocialAccountUnlinkClient;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class RestSocialAccountUnlinkClient implements SocialAccountUnlinkClient {

  private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";
  private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

  private final RestClient restClient;

  public RestSocialAccountUnlinkClient(RestClient.Builder restClientBuilder) {
    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(3));
    requestFactory.setReadTimeout(Duration.ofSeconds(5));
    this.restClient = restClientBuilder
        .requestFactory(requestFactory)
        .build();
  }

  @Override
  public void unlink(Provider provider, String accessToken) {
    Objects.requireNonNull(provider, "provider is required");
    if (accessToken == null || accessToken.isBlank()) {
      throw new BaseException(ErrorCode.SOCIAL_UNLINK_FAILED);
    }
    if (provider == Provider.LOCAL) {
      throw new BaseException(ErrorCode.INVALID_REQUEST);
    }

    try {
      switch (provider) {
        case GOOGLE -> revokeGoogle(accessToken);
        case KAKAO -> unlinkKakao(accessToken);
        case LOCAL -> throw new IllegalStateException("LOCAL provider cannot be unlinked");
      }
    } catch (RestClientException exception) {
      log.warn("Failed to unlink {} OAuth account", provider, exception);
      throw new BaseException(ErrorCode.SOCIAL_UNLINK_FAILED, exception);
    }
  }

  private void revokeGoogle(String accessToken) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("token", accessToken);

    restClient.post()
        .uri(GOOGLE_REVOKE_URL)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .toBodilessEntity();
  }

  private void unlinkKakao(String accessToken) {
    restClient.post()
        .uri(KAKAO_UNLINK_URL)
        .headers(headers -> headers.setBearerAuth(accessToken))
        .retrieve()
        .toBodilessEntity();
  }
}
