package com.codeit.modoo_playlist.moduleapi.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.codeit.modoo_playlist.moduleapi.security.SecurityErrorResponseWriter;
import com.codeit.modoo_playlist.moduleapi.security.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;

class JwtAuthenticationFilterTest {

  @Test
  void skipsPasswordResetEndpoint() {
    JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
        mock(JwtTokenProvider.class),
        mock(UserDetailsService.class),
        mock(SecurityErrorResponseWriter.class),
        mock(LoginSessionStore.class),
        mock(RoleHierarchy.class)
    );
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/auth/reset-password");

    assertThat(filter.shouldNotFilter(request)).isTrue();
  }
}
