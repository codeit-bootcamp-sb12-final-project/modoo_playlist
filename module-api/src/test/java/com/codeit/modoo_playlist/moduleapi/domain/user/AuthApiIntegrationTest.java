package com.codeit.modoo_playlist.moduleapi.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.moduleapi.domain.message.repository.MessageRepository;
import com.codeit.modoo_playlist.moduleapi.domain.review.repository.ReviewRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.watchingsession.repository.WatchingSessionRepository;
import com.codeit.modoo_playlist.moduleapi.dto.jwt.LoginSession;
import com.codeit.modoo_playlist.core.global.security.LoginSession;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import com.codeit.modoo_playlist.moduleapi.security.jwt.RefreshTokenHasher;
import com.codeit.modoo_playlist.moduleapi.support.AuthTestApplication;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = AuthTestApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

  private static final String PASSWORD = "Password123!";
  private static final String USERNAME = "tester";
  private static final String CHANGED_USERNAME = "changed";

  @Autowired
  MockMvc mvc;

  @Autowired
  UserRepository users;

  @Autowired
  PasswordEncoder passwordEncoder;

  @Autowired
  JwtTokenProvider tokens;

  @Autowired
  LoginSessionStore sessions;

  @Autowired
  RefreshTokenHasher hasher;

  @Autowired
  JsonMapper json;

  @Autowired
  StringRedisTemplate redis;

  @MockitoBean
  MessageRepository messageRepository;

  @MockitoBean
  WatchingSessionRepository watchingSessionRepository;

  @MockitoBean
  ReviewRepository reviewRepository;

  private String email;
  private Map<String, String> signupRequest;

  @BeforeEach
  void uniqueAccount() {
    email = UUID.randomUUID() + "@example.com";
    signupRequest = Map.of("email", email, "name", USERNAME, "password", PASSWORD);
  }

  @Test
  @DisplayName("회원가입은 DB에 암호화 비밀번호를 저장.")
  void signupPersistsEncodedPassword() throws Exception {
    MockHttpServletResponse response = signup().andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.locked").value(false))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.tempPassword").doesNotExist())
        .andReturn().getResponse();
    User stored = users.findByEmail(email).orElseThrow();

    assertThat(stored.getId().toString()).isEqualTo(body(response).get("id").asText());
    assertThat(stored.getCreatedAt()).isNotNull();
    assertThat(stored.getPassword()).isNotEqualTo(PASSWORD);
    assertThat(passwordEncoder.matches(PASSWORD, stored.getPassword())).isTrue();
    assertThat(passwordEncoder.matches("wrong", stored.getPassword())).isFalse();
  }

  @Test
  @DisplayName("이메일 중복은 EMAIL_ALREADY_EXISTS 반환")
  void duplicateEmail() throws Exception {
    signup().andExpect(status().isOk());
    long before = users.count();

    signup().andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(
            result -> assertThat(result.getResolvedException()).isInstanceOf(BaseException.class));

    assertThat(users.count()).isEqualTo(before);
  }

  @Test
  @DisplayName("DB의 이메일 UNIQUE 제약")
  void databaseRejectsDuplicateEmail() throws Exception {
    signup().andExpect(status().isOk());

    assertThatThrownBy(() -> users.saveAndFlush(User.create(email, "duplicate", "hash")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  static Stream<Map<String, String>> invalidSignupRequests() {
    return Stream.of(
        Map.of("email", "invalid", "name", "name", "password", PASSWORD),
        Map.of("email", "", "name", "name", "password", PASSWORD),
        Map.of("name", "name", "password", PASSWORD),
        Map.of("email", "valid@example.com", "name", "", "password", PASSWORD),
        Map.of("email", "valid@example.com", "name", "a".repeat(51), "password", PASSWORD),
        Map.of("email", "valid@example.com", "name", "name", "password", ""),
        Map.of("email", "valid@example.com", "name", "name", "password", "a".repeat(256)));
  }

  //  위의 정적 stream map을 넘겨서 invalid 값들을 한번에 테스트.
  @ParameterizedTest
  @MethodSource("invalidSignupRequests")
  @DisplayName("회원가입 입력 검증 실패는 400.")
  void invalidSignup(Map<String, String> request) throws Exception {
    long before = users.count();

    mvc.perform(withCsrf(post("/api/users")).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details").isNotEmpty());

    assertThat(users.count()).isEqualTo(before);
  }

  @Test
  @DisplayName("사용자 상세 조회와 미존재 사용자 조회.")
  void userLookup() throws Exception {
    signup().andExpect(status().isOk());

    UUID id = users.findByEmail(email).orElseThrow().getId();

//    있는 사용자 조회
    mvc.perform(get("/api/users/{id}", id)).andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.password").doesNotExist());

//    없는 사용자 조히
    mvc.perform(get("/api/users/{id}", UUID.randomUUID())).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        .andExpect(
            result -> assertThat(result.getResolvedException()).isInstanceOf(BaseException.class));
  }

  @Test
  @DisplayName("로그인은 access JWT, HttpOnly refresh 쿠키, 해시된 Redis 세션을 발급.")
  void loginIssuesTokensAndSession() throws Exception {
    Login login = registeredLogin();

    assertThat(tokens.validateAccessToken(login.access())).isTrue();
    assertThat(tokens.validateRefreshToken(login.refresh().getValue())).isTrue();
    assertThat(tokens.getUserId(login.access())).isEqualTo(login.userId());
    assertThat(tokens.getSid(login.access())).isEqualTo(tokens.getSid(login.refresh().getValue()));

    LoginSession session = sessions.findActive(login.userId(), tokens.getSid(login.access()))
        .orElseThrow();

    assertThat(session.refreshTokenHash()).isEqualTo(hasher.hash(login.refresh().getValue()))
        .isNotEqualTo(login.refresh().getValue());
    assertThat(session.expiresAt()).isAfter(Instant.now());
    assertThat(login.refresh().isHttpOnly()).isTrue();
    assertThat(login.refresh().getSecure()).isFalse();
    assertThat(login.refresh().getPath()).isEqualTo("/");
    assertThat(login.refresh().getAttribute("SameSite")).isEqualTo("Lax");
    assertThat(login.refresh().getMaxAge()).isBetween(1, 3600);
  }

  @Test
  @DisplayName("계정 없음과 비밀번호 불일치 401 반환.")
  void invalidCredentials() throws Exception {
    Login login = registeredLogin();

    LoginSession before = sessions.findActive(login.userId(), tokens.getSid(login.access()))
        .orElseThrow();

    MockHttpServletResponse wrong = signIn(email, "wrong").andExpect(status().isUnauthorized())
        .andExpect(cookie().doesNotExist("REFRESH_TOKEN"))
        .andExpect(jsonPath("$.accessToken").doesNotExist()).andReturn().getResponse();

    MockHttpServletResponse missing = signIn(UUID.randomUUID() + "@example.com", PASSWORD)
        .andExpect(status().isUnauthorized()).andExpect(cookie().doesNotExist("REFRESH_TOKEN"))
        .andReturn().getResponse();

    assertThat(body(wrong).get("message")).isEqualTo(body(missing).get("message"));
    assertThat(sessions.findActive(login.userId(), tokens.getSid(login.access()))).contains(before);
  }

  @Test
  @DisplayName("발급된 access JWT로 본인 프로필을 수정.")
  void updateOwnProfile() throws Exception {
    Login login = registeredLogin();

    mvc.perform(withCsrf(profile(login.userId(), CHANGED_USERNAME))
            .header("Authorization", "Bearer " + login.access()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(CHANGED_USERNAME));

    assertThat(users.findById(login.userId()).orElseThrow().getUsername())
        .isEqualTo(CHANGED_USERNAME);
  }

  @Test
  @DisplayName("미인증 수정은 401, 다른 사용자 수정은 ACCESS_DENIED 403")
  void updateRequiresAuthenticationAndOwnership() throws Exception {
    Login login = registeredLogin();

    mvc.perform(withCsrf(profile(login.userId(), CHANGED_USERNAME)))
        .andExpect(status().isUnauthorized());

    String otherEmail = UUID.randomUUID() + "@example.com";
    signup(otherEmail).andExpect(status().isOk());
    UUID otherId = users.findByEmail(otherEmail).orElseThrow().getId();

    mvc.perform(withCsrf(profile(otherId, CHANGED_USERNAME))
            .header("Authorization", "Bearer " + login.access()))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

    assertThat(users.findById(otherId).orElseThrow().getUsername()).isEqualTo(USERNAME);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"})
  @DisplayName("유효하지 않은 프로필 수정은 400.")
  void invalidProfile(String name) throws Exception {
    Login login = registeredLogin();

    mvc.perform(
            withCsrf(profile(login.userId(), name)).header("Authorization", "Bearer " + login.access()))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    assertThat(users.findById(login.userId()).orElseThrow().getUsername()).isEqualTo(USERNAME);
  }

  @Test
  @DisplayName("CSRF 쿠키 발급 후 쿠키와 헤더가 일치해야 쓰기 요청을 허용.")
  void csrfRequired() throws Exception {
    mvc.perform(post("/api/auth/sign-in").param("username", email).param("password", PASSWORD))
        .andExpect(status().isForbidden());

    Cookie csrf = csrfCookie();

    assertThat(csrf.isHttpOnly()).isFalse();

    mvc.perform(post("/api/auth/sign-in").cookie(csrf).header("X-XSRF-TOKEN", "wrong")
            .param("username", email).param("password", PASSWORD))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("변조된 access JWT와 정상refresh에 타입이 access인 경우 401")
  void rejectsInvalidAccessTokens() throws Exception {
    Login login = registeredLogin();

    for (String token : new String[]{"malformed.jwt.token", login.refresh().getValue()}) {
      mvc.perform(withCsrf(profile(login.userId(), CHANGED_USERNAME))
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }
  }

  @Test
  @DisplayName("새 요청에서 refresh 쿠키만으로 로그인 복원 및 토큰을 회전.")
  void restoreLoginWithoutAccessToken() throws Exception {
    Login login = registeredLogin();

    MockHttpServletResponse response = refresh(login.refresh()).andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.userDto.id").value(login.userId().toString()))
        .andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn().getResponse();

    String access = body(response).get("accessToken").asText();
    Cookie renewed = response.getCookie("REFRESH_TOKEN");

    assertThat(access).isNotEqualTo(login.access());
    assertThat(renewed.getValue()).isNotEqualTo(login.refresh().getValue());
    assertThat(tokens.getSid(access)).isEqualTo(tokens.getSid(login.access()));
    assertThat(
        sessions.findActive(login.userId(), tokens.getSid(access)).orElseThrow().refreshTokenHash())
        .isEqualTo(hasher.hash(renewed.getValue()));

    mvc.perform(
            withCsrf(profile(login.userId(), "restored")).header("Authorization", "Bearer " + access))
        .andExpect(status().isOk());

    refresh(login.refresh()).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    refresh(renewed).andExpect(status().isOk());
  }

  @Test
  @DisplayName("누락되거나 잘못된 refresh 쿠키는 INVALID_TOKEN")
  void invalidRefreshCookie() throws Exception {
    mvc.perform(withCsrf(post("/api/auth/refresh"))).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

    refresh(new Cookie("REFRESH_TOKEN", "invalid")).andExpect(status().isUnauthorized())
        .andExpect(cookie().doesNotExist("REFRESH_TOKEN"));
  }

  @Test
  @DisplayName("재로그인은 이전 세션을 무효화. 이전 로그아웃이 새 세션을 지우지 않는다.")
  void secondLoginReplacesSession() throws Exception {
    Login old = registeredLogin();

    Login current = readLogin(
        signIn(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse());

    assertThat(sessions.findActive(old.userId(), tokens.getSid(old.access()))).isEmpty();

    refresh(old.refresh()).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("LOGIN_SESSION_INVALIDATED"));

    mvc.perform(withCsrf(profile(old.userId(), "rejected")).header("Authorization",
            "Bearer " + old.access()))
        .andExpect(status().isUnauthorized());

    mvc.perform(withCsrf(post("/api/auth/sign-out")).cookie(old.refresh()))
        .andExpect(status().isNoContent());

    refresh(current.refresh()).andExpect(status().isOk());
  }

  @Test
  @DisplayName("로그아웃은 쿠키와 서버 세션을 제거. access 및 refresh 재사용을 거부.")
  void logoutInvalidatesSession() throws Exception {
    Login login = registeredLogin();
    mvc.perform(withCsrf(post("/api/auth/sign-out")).cookie(login.refresh()))
        .andExpect(status().isNoContent()).andExpect(cookie().maxAge("REFRESH_TOKEN", 0))
        .andExpect(cookie().path("REFRESH_TOKEN", "/"));
    assertThat(sessions.findActive(login.userId(), tokens.getSid(login.access()))).isEmpty();
    mvc.perform(withCsrf(profile(login.userId(), "rejected")).header("Authorization",
            "Bearer " + login.access()))
        .andExpect(status().isUnauthorized());
    refresh(login.refresh()).andExpect(status().isUnauthorized());
    mvc.perform(withCsrf(post("/api/auth/sign-out"))).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("refresh 쿠키 없이 유효한 access 헤더만으로도 해당 세션을 로그아웃 가능.")
  void logoutUsingAccessToken() throws Exception {
    Login login = registeredLogin();

    mvc.perform(
            withCsrf(post("/api/auth/sign-out")).header("Authorization", "Bearer " + login.access()))
        .andExpect(status().isNoContent());

    assertThat(sessions.findActive(login.userId(), tokens.getSid(login.access()))).isEmpty();
  }

  @Test
  @DisplayName("JWT가 유효해도 저장된 세션이 만료되면 인증과 복원을 거부.")
  void expiredSession() throws Exception {
    Login login = registeredLogin();

    // 만료 시각을 직접 저장하여 sleep 없이 세션 만료 경계를 검증한다.
    LoginSession expired = new LoginSession(tokens.getSid(login.access()), login.userId(),
        hasher.hash(login.refresh().getValue()), Instant.now().minusSeconds(7200),
        Instant.now().minusSeconds(1));

    redis.opsForValue().set("auth:session:" + login.userId(), json.writeValueAsString(expired));

    mvc.perform(withCsrf(profile(login.userId(), "rejected")).header("Authorization",
            "Bearer " + login.access()))
        .andExpect(status().isUnauthorized());

    refresh(login.refresh()).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("LOGIN_SESSION_INVALIDATED"));
  }

  @Test
  @DisplayName("손상된 refresh 쿠키도 유효한 access 헤더가 있으면 서버 세션을 삭제.")
  void logoutFallsBackFromDamagedRefresh() throws Exception {
    Login login = registeredLogin();

    mvc.perform(withCsrf(post("/api/auth/sign-out"))
            .cookie(new Cookie("REFRESH_TOKEN", "damaged"))
            .header("Authorization", "Bearer " + login.access()))
        .andExpect(status().isNoContent()).andExpect(cookie().maxAge("REFRESH_TOKEN", 0));

    assertThat(sessions.findActive(login.userId(), tokens.getSid(login.access()))).isEmpty();
  }

  @Test
  @DisplayName("식별 가능한 토큰이 없으면 쿠키만 제거.")
  void logoutWithoutIdentityRetainsSession() throws Exception {
    Login login = registeredLogin();

    mvc.perform(withCsrf(post("/api/auth/sign-out"))
            .cookie(new Cookie("REFRESH_TOKEN", "damaged")))
        .andExpect(status().isNoContent()).andExpect(cookie().maxAge("REFRESH_TOKEN", 0));

    assertThat(sessions.findActive(login.userId(), tokens.getSid(login.access()))).isPresent();
    assertThat(redis.getExpire("auth:session:" + login.userId())).isPositive();
  }

  //  최초 이메일 가입만 허용. 따로 락을 거는게 아니라 db에서 unique로 관리.
  @Test
  @DisplayName("동일 이메일 동시 가입은 한 건만 저장하고 나머지는 409로 응답한다")
  void concurrentSignup() throws Exception {
    Cookie csrf = csrfCookie();

    String payload = json.writeValueAsString(signupRequest);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CyclicBarrier barrier = new CyclicBarrier(2);

    java.util.concurrent.Callable<Integer> signup = () -> {
      barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
      return mvc.perform(post("/api/users").cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
              .contentType(MediaType.APPLICATION_JSON).content(payload))
          .andReturn().getResponse().getStatus();
    };

    try {
      Future<Integer> first = pool.submit(signup);
      Future<Integer> second = pool.submit(signup);
      assertThat(java.util.List.of(first.get(20, java.util.concurrent.TimeUnit.SECONDS),
          second.get(20, java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,
          409);
      assertThat(
          users.findAll().stream().filter(user -> user.getEmail().equals(email)).count()).isEqualTo(
          1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  @DisplayName("일반 사용자는 관리자 사용자 목록 API 호출 불가")
  void normalUserCannotCallAdminApi() throws Exception {
    Login login = registeredLogin();

    mvc.perform(get("/api/users")
            .param("limit", "20")
            .param("sortDirection", "ASCENDING")
            .param("sortBy", "email")
            .header("Authorization", "Bearer " + login.access()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  @DisplayName("일반 사용자는 관리자 사용자 삭제 API를 호출할 수 없다")
  void normalUserCannotDeleteUser() throws Exception {
    Login actor = registeredLogin();
    UUID targetId = UUID.randomUUID();

    mvc.perform(withCsrf(delete("/api/users/{userId}/purge", targetId))
            .header("Authorization", "Bearer " + actor.access()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  @DisplayName("관리자가 탈퇴 사용자를 영구 삭제하면 기존 세션을 무효화한다")
  void adminPurgesWithdrawnUserAndInvalidatesSession() throws Exception {
    Login target = registeredLogin();
    Login admin = adminLogin();
    User withdrawnUser = users.findById(target.userId()).orElseThrow();
    withdrawnUser.withdraw(Instant.now());
    users.saveAndFlush(withdrawnUser);

    mvc.perform(withCsrf(delete("/api/users/{userId}/purge", target.userId()))
            .header("Authorization", "Bearer " + admin.access()))
        .andExpect(status().isNoContent());

    assertThat(users.findById(target.userId())).isEmpty();
    mvc.perform(withCsrf(profile(target.userId(), "rejected"))
            .header("Authorization", "Bearer " + target.access()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("LOGIN_SESSION_INVALIDATED"));
  }

  @Test
  @DisplayName("관리자가 존재하지 않는 사용자를 수정하면 USER_NOT_FOUND.")
  void adminCannotUpdateMissingUser() throws Exception {
    Login admin = adminLogin();

    mvc.perform(withCsrf(patch("/api/users/{userId}/role", UUID.randomUUID()))
            .header("Authorization", "Bearer " + admin.access())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("role", "ADMIN"))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  @Test
  @DisplayName("로그인 중인 사용자를 잠그면 바로 세션 무효화.")
  void lockingLoggedInUserInvalidatesSessionAndRejectsLogin() throws Exception {
    Login target = registeredLogin();
    Login admin = adminLogin();

    mvc.perform(withCsrf(patch("/api/users/{userId}/locked", target.userId()))
            .header("Authorization", "Bearer " + admin.access())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("locked", true))))
        .andExpect(status().isNoContent());

    mvc.perform(withCsrf(profile(target.userId(), "rejected"))
            .header("Authorization", "Bearer " + target.access()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("LOGIN_SESSION_INVALIDATED"));

    signIn(email, PASSWORD)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("USER_ACCOUNT_LOCKED"));
  }

  @Test
  @DisplayName("로그인 중 역할을 변경시 세션 무효화.")
  void promotingLoggedInUserRequiresLoginAgain() throws Exception {
    Login target = registeredLogin();
    Login admin = adminLogin();

    mvc.perform(withCsrf(patch("/api/users/{userId}/role", target.userId()))
            .header("Authorization", "Bearer " + admin.access())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("role", "ADMIN"))))
        .andExpect(status().isNoContent());

    mvc.perform(withCsrf(profile(target.userId(), "rejected"))
            .header("Authorization", "Bearer " + target.access()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("LOGIN_SESSION_INVALIDATED"));

    Login promoted = readLogin(
        signIn(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse());

    mvc.perform(get("/api/users")
            .param("limit", "20")
            .param("sortDirection", "ASCENDING")
            .param("sortBy", "email")
            .header("Authorization", "Bearer " + promoted.access()))
        .andExpect(status().isOk());
  }

  private ResultActions signup() throws Exception {
    return mvc.perform(withCsrf(post("/api/users")).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(signupRequest)));
  }

  private ResultActions signup(String address) throws Exception {
    return mvc.perform(withCsrf(post("/api/users")).contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(
            Map.of("email", address, "name", USERNAME, "password", PASSWORD))));
  }

  private ResultActions signIn(String address, String password) throws Exception {
    return mvc.perform(withCsrf(post("/api/auth/sign-in"))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("username", address)
        .param("password", password));
  }

  private Login registeredLogin() throws Exception {
    signup().andExpect(status().isOk());
    return readLogin(signIn(email, PASSWORD).andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.userDto.password").doesNotExist()).andReturn().getResponse());
  }

  private Login adminLogin() throws Exception {
    String adminEmail = UUID.randomUUID() + "@example.com";
    signup(adminEmail).andExpect(status().isOk());
    User admin = users.findByEmail(adminEmail).orElseThrow();
    admin.changeRole(UserRole.ADMIN);
    users.saveAndFlush(admin);
    return readLogin(
        signIn(adminEmail, PASSWORD).andExpect(status().isOk()).andReturn().getResponse());
  }

  private Login readLogin(MockHttpServletResponse response) throws Exception {
    JsonNode body = body(response);
    return new Login(UUID.fromString(body.get("userDto").get("id").asText()),
        body.get("accessToken").asText(), response.getCookie("REFRESH_TOKEN"));
  }

  private ResultActions refresh(Cookie cookie) throws Exception {
    return mvc.perform(withCsrf(post("/api/auth/refresh")).cookie(cookie));
  }

  private MockMultipartHttpServletRequestBuilder profile(UUID id, String name) {
    return multipart(HttpMethod.PATCH, "/api/users/{id}", id)
        .file(new MockMultipartFile("request", "", "application/json",
            json.writeValueAsString(Map.of("name", name)).getBytes(StandardCharsets.UTF_8)));
  }

  private Cookie csrfCookie() throws Exception {
    return mvc.perform(get("/api/auth/csrf-token")).andExpect(status().isNoContent())
        .andExpect(cookie().exists("XSRF-TOKEN")).andReturn().getResponse().getCookie("XSRF-TOKEN");
  }

  private <T extends AbstractMockHttpServletRequestBuilder<T>> T withCsrf(T request)
      throws Exception {
    Cookie csrf = csrfCookie();
    return request.cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue());
  }

  private JsonNode body(MockHttpServletResponse response) throws Exception {
    return json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
  }

  private record Login(UUID userId, String access, Cookie refresh) {

  }
}
