package com.codeit.modoo_playlist.moduleapi.security.jwt;

import com.codeit.modoo_playlist.modulerealtime.config.WebSocketSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketHttpSecurityTest {
    @Test
    void sockJsPostDoesNotRequireCsrfButOtherPathsRemainProtected() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(WebSocketSecurityConfig.class);
            context.refresh();
            FilterChainProxy security = context.getBean("springSecurityFilterChain", FilterChainProxy.class);

            assertThat(request(security, "GET", "/ws").getStatus()).isEqualTo(204);
            assertThat(request(security, "GET", "/ws/info").getStatus()).isEqualTo(204);
            assertThat(request(security, "POST", "/ws/000/session/xhr_send").getStatus()).isEqualTo(204);
            assertThat(request(security, "POST", "/api/example").getStatus()).isEqualTo(403);
            assertThat(request(security, "GET", "/api/example").getStatus()).isEqualTo(403);
        }
    }

    private MockHttpServletResponse request(FilterChainProxy security, String method, String path)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Reaching the terminal chain proves HTTP security accepted the request;
        // this test deliberately does not emulate a SockJS transport server.
        security.doFilter(request, response, (req, res) ->
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(204));
        return response;
    }
}
