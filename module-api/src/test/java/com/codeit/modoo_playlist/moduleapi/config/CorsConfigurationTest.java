package com.codeit.modoo_playlist.moduleapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.modoo_playlist.moduleapi.config.properties.CorsProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;

class CorsConfigurationTest {

  private static final String ALLOWED_ORIGIN = "http://localhost:5173";

  private CorsConfiguration configuration;

  @BeforeEach
  void setUp() {
    CorsProperties properties = new CorsProperties(List.of(ALLOWED_ORIGIN));
    CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource(properties);
    configuration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/users"));
  }

  @Test
  void allowsConfiguredOriginWithCredentials() throws Exception {
    MockHttpServletRequest request = preflightRequest(ALLOWED_ORIGIN);
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean accepted = new DefaultCorsProcessor().processRequest(configuration, request, response);

    assertThat(accepted).isTrue();
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        .isEqualTo(ALLOWED_ORIGIN);
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
        .isEqualTo("true");
  }

  @Test
  void rejectsOriginThatIsNotConfigured() throws Exception {
    MockHttpServletRequest request = preflightRequest("https://attacker.example");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean accepted = new DefaultCorsProcessor().processRequest(configuration, request, response);

    assertThat(accepted).isFalse();
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
  }

  private MockHttpServletRequest preflightRequest(String origin) {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/users");
    request.addHeader(HttpHeaders.ORIGIN, origin);
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
    return request;
  }
}
