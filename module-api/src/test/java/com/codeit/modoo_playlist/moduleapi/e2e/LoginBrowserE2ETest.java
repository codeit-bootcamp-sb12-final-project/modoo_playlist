package com.codeit.modoo_playlist.moduleapi.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.modoo_playlist.moduleapi.domain.user.service.UserService;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import com.codeit.modoo_playlist.core.global.security.LoginSessionStore;
import com.codeit.modoo_playlist.moduleapi.support.AuthTestApplication;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

@Tag("e2e")
@SpringBootTest(classes = AuthTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoginBrowserE2ETest {

  @LocalServerPort
  int port;

  @Autowired
  UserService users;

  @Autowired
  JwtTokenProvider tokens;

  @Autowired
  LoginSessionStore sessions;

  @Autowired
  JsonMapper json;

  private Playwright playwright;
  private Browser browser;
  private BrowserContext context;
  private Page page;
  private String email;
  private UUID userId;
  private static final String PASSWORD = "Password123!";

  @BeforeEach
  void setUp() {
    email = UUID.randomUUID() + "@example.com";
    userId = users.create(new UserCreateRequest(email, "browser-user", PASSWORD)).id();
    playwright = Playwright.create();
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true)
        .setChannel(System.getProperty("e2e.browser.channel", "chromium")));
    context = browser.newContext();
    page = context.newPage();
    page.setDefaultTimeout(15000);
    // 콘텐츠 목록·알림은 로그인 테스트 범위 밖이다. Auth/User 요청은 가로채지 않는다.
    page.route("**/api/**", route -> {
      String path = java.net.URI.create(route.request().url()).getPath();
      if (path.startsWith("/api/auth/") || path.startsWith("/api/users")) {
        route.resume();
      } else {
        route.fulfill(new Route.FulfillOptions().setContentType("application/json")
            .setBody(
                "{\"data\":[],\"content\":[],\"hasNext\":false,\"totalCount\":0,\"nextCursor\":null}"));
      }
    });
  }

  @AfterEach
  void close() {
    if (context != null) {
      context.close();
    }
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  @Test
  @DisplayName("로그인 후 메인 이동, HttpOnly 쿠키 저장, 새로고침 시 refresh 쿠키로 복원")
  void loginAndReload() {
    String access = login();
    Cookie refresh = refreshCookie();

    assertThat(refresh.httpOnly).isTrue();
    assertThat(refresh.path).isEqualTo("/");
    assertThat(refresh.secure).isFalse();
    assertThat(tokens.validateAccessToken(access)).isTrue();
    assertThat(tokens.validateRefreshToken(refresh.value)).isTrue();
    assertThat((String) page.evaluate("document.cookie")).doesNotContain("REFRESH_TOKEN=");

    Response restored = page.waitForResponse(
        r -> r.url().endsWith("/api/auth/refresh") && r.request().method().equals("POST"),
        () -> page.reload());

    assertThat(restored.status()).isEqualTo(200);
    assertThat(restored.request().allHeaders().get("cookie")).contains(
        "REFRESH_TOKEN=" + refresh.value);
    assertThat(restored.request().allHeaders().get("x-xsrf-token")).isNotBlank();
    assertThat(page).hasURL(base() + "/#/contents");
    assertThat(page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Profile"))).isVisible();
    assertThat(refreshCookie().value).isNotEqualTo(refresh.value);

    String renewedAccess = json.readTree(restored.text()).get("accessToken").asText();

    assertThat(tokens.getUserId(renewedAccess)).isEqualTo(userId);

    // 실제 프런트엔드의 사용자 조회 요청에서 Authorization 전달을 확인한다.
    Response profile = page.waitForResponse(r -> r.url().endsWith("/api/users/" + userId), () -> {
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Profile")).click();
      page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("프로필").setExact(true))
          .click();
    });

    assertThat(profile.status()).isEqualTo(200);
    assertThat(profile.request().allHeaders().get("authorization")).isEqualTo(
        "Bearer " + renewedAccess);
  }

  @Test
  @DisplayName("UI 로그아웃은 쿠키·Redis 세션을 제거. 새로고침해도 복원 안됨.")
  void logoutAndReload() {
    String access = login();

    Response logout = page.waitForResponse(r -> r.url().endsWith("/api/auth/sign-out"), () -> {
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Profile")).click();
      page.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("로그아웃")).click();
    });

    assertThat(logout.status()).isEqualTo(204);
    assertThat(logout.request().allHeaders().get("authorization")).isEqualTo("Bearer " + access);
    assertThat(page).hasURL(base() + "/#/sign-in");
    assertThat(context.cookies().stream().anyMatch(c -> c.name.equals("REFRESH_TOKEN"))).isFalse();
    assertThat(sessions.findActive(userId, tokens.getSid(access))).isEmpty();

    page.reload();

    assertThat(page.locator("#email")).isVisible();
    assertThat(page).hasURL(base() + "/#/sign-in");
  }

  @Test
  @DisplayName("무효화한 세션은 새로고침 시 복원 안됨.")
  void invalidatedSession() {
    String access = login();

    sessions.invalidate(userId, tokens.getSid(access));

    Response restored = page.waitForResponse(r -> r.url().endsWith("/api/auth/refresh"),
        () -> page.reload());

    assertThat(restored.status()).isEqualTo(401);
    assertThat(page).hasURL(base() + "/#/sign-in");
    assertThat(page.locator("#email")).isVisible();
  }

  //  프론트에서 로그인 실패시 요청중인 이메일과 비밀번호가 지워지지 않고 프론트에 그대로 있는거 말하는 중.
  @Test
  @DisplayName("잘못된 비밀번호는 로그인 화면 유지.")
  void wrongPassword() {
    openLogin();

    page.locator("#email").fill(email);
    page.locator("#password").fill("wrong-password");

    Response response = page.waitForResponse(r -> r.url().endsWith("/api/auth/sign-in"),
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("로그인").setExact(true)).click());

    assertThat(response.status()).isEqualTo(401);
    assertThat(page).hasURL(base() + "/#/sign-in");
    assertThat(context.cookies().stream().anyMatch(c -> c.name.equals("REFRESH_TOKEN"))).isFalse();
  }

  private void openLogin() {
    page.navigate(base() + "/#/sign-in");
    assertThat(page.locator("#email")).isVisible();
    // 초기 CSRF 조회 완료 후 제출한다.
    page.waitForFunction("document.cookie.includes('XSRF-TOKEN=')");
  }

  @ParameterizedTest
  @ValueSource(strings = {"missing", "damaged"})
  @DisplayName("refresh 쿠키 누락·손상 시 브라우저 로그인 복원을 거부. 서버 세션은 유지.")
  void unusableRefreshCookie(String scenario) {
    String access = login();

    context.clearCookies(new BrowserContext.ClearCookiesOptions().setName("REFRESH_TOKEN"));

    if (scenario.equals("damaged")) {
      context.addCookies(java.util.List.of(new Cookie("REFRESH_TOKEN", "damaged")
          .setUrl(base()).setHttpOnly(true)));
    }

    Response restored = page.waitForResponse(r -> r.url().endsWith("/api/auth/refresh"),
        () -> page.reload());

    assertThat(restored.status()).isEqualTo(401);
    assertThat(page).hasURL(base() + "/#/sign-in");
    assertThat(sessions.findActive(userId, tokens.getSid(access))).isPresent();
  }

  private String login() {
    openLogin();
    page.locator("#email").fill(email);
    page.locator("#password").fill(PASSWORD);
    Response response = page.waitForResponse(r -> r.url().endsWith("/api/auth/sign-in"),
        () -> page.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("로그인").setExact(true)).click());
    assertThat(response.status()).isEqualTo(200);
    assertThat(page).hasURL(base() + "/#/contents");
    return json.readTree(response.text()).get("accessToken").asText();
  }

  private Cookie refreshCookie() {
    return context.cookies().stream().filter(c -> c.name.equals("REFRESH_TOKEN")).findFirst()
        .orElseThrow();
  }

  private String base() {
    return "http://localhost:" + port;
  }
}
