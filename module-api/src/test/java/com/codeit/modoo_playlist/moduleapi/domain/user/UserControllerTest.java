package com.codeit.modoo_playlist.moduleapi.domain.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.modoo_playlist.core.domain.user.entity.Provider;
import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.user.controller.UserController;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.UserService;
import com.codeit.modoo_playlist.moduleapi.domain.user.service.OAuthWithdrawalService;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserCreateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserProfileUpdateRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserWithdrawalRequest;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.WithdrawalInfoResponse;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.WithdrawalVerificationMethod;
import com.codeit.modoo_playlist.moduleapi.dto.user.response.OAuthWithdrawalAuthorizationResponse;
import com.codeit.modoo_playlist.moduleapi.exception.GlobalExceptionHandler;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import com.codeit.modoo_playlist.moduleapi.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

/**
 * 요청 바인딩·검증·응답만 검증한다. 보안 필터는 AuthApiIntegrationTest에서 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  private static final String EMAIL = "user@example.com";
  private static final String USERNAME = "tester";
  private static final String PASSWORD = "Password123!";
  private static final String CHANGED_USERNAME = "changed";

  @Mock
  UserService userService;

  @Mock
  JwtTokenProvider jwtTokenProvider;

  @Mock
  OAuthWithdrawalService oauthWithdrawalService;

  private MockMvc mvc;
  private final JsonMapper json = JsonMapper.builder().build();
  private UUID id;
  private UserDto dto;
  private UserCreateRequest createRequest;
  private UserProfileUpdateRequest updateRequest;

  @BeforeEach
  void setUp() {
    id = UUID.randomUUID();
    dto = new UserDto(id, EMAIL, USERNAME, null, UserRole.USER, false, Instant.now());
    createRequest = new UserCreateRequest(EMAIL, USERNAME, PASSWORD);
    updateRequest = new UserProfileUpdateRequest(CHANGED_USERNAME);
    mvc = MockMvcBuilders.standaloneSetup(
            new UserController(userService, oauthWithdrawalService, jwtTokenProvider))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("회원가입. DTO 반환.")
  void signup() throws Exception {
    when(userService.create(createRequest)).thenReturn(dto);

    mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(createRequest)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.name").value(USERNAME))
        .andExpect(jsonPath("$.password").doesNotExist());

    verify(userService).create(createRequest);
  }

  //  잘못된 이메일은 프론트에서 필터링이 1차적으로 되고, 내부에서 추가 검증
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "invalid-email"})
  @DisplayName("잘못된 이메일은 서비스 호출 전에 VALIDATION_ERROR 400")
  void invalidEmail(String email) throws Exception {
    UserCreateRequest request = new UserCreateRequest(email, USERNAME, PASSWORD);

    mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(request)))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details.email").exists());

    verifyNoInteractions(userService);
  }

  @ParameterizedTest
  @ValueSource(ints = {50, 51})
  @DisplayName("회원가입 이름의 50자 초과 검증")
  void signupNameBoundary(int length) throws Exception {
    UserCreateRequest request = new UserCreateRequest(EMAIL, "a".repeat(length), PASSWORD);

    if (length == 50) {
      when(userService.create(request)).thenReturn(dto);
    }

    ResultActions response = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(request)));

    if (length == 50) {
      response.andExpect(status().isOk());
      verify(userService).create(request);
    } else {
      response.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.name").exists());
      verifyNoInteractions(userService);
    }
  }

  @Test
  @DisplayName("이메일 중복 회원가입 시 에러.")
  void duplicateEmailResponse() throws Exception {
    when(userService.create(any())).thenThrow(new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS));

    mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(createRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
  }

  @Test
  @DisplayName("유저 조회")
  void lookup() throws Exception {
    when(userService.getUser(id)).thenReturn(dto);

    mvc.perform(get("/api/users/{id}", id)).andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(dto.email()));

    verify(userService).getUser(id);
  }

  @Test
  @DisplayName("회원 탈퇴 인증 방식 조회")
  void getWithdrawalInfo() throws Exception {
    authenticate();
    WithdrawalInfoResponse response = new WithdrawalInfoResponse(
        WithdrawalVerificationMethod.OAUTH,
        Provider.GOOGLE
    );
    when(userService.getWithdrawalInfo(id)).thenReturn(response);

    mvc.perform(get("/api/users/me/withdrawal-info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.verificationMethod").value("OAUTH"))
        .andExpect(jsonPath("$.provider").value("GOOGLE"));

    verify(userService).getWithdrawalInfo(id);
  }

  @Test
  @DisplayName("일반 계정 탈퇴 후 refresh 쿠키를 만료한다")
  void withdrawPasswordAccount() throws Exception {
    authenticate();
    UserWithdrawalRequest request = new UserWithdrawalRequest(PASSWORD);
    Cookie expiredCookie = new Cookie(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME, "");
    expiredCookie.setMaxAge(0);
    when(jwtTokenProvider.generateRefreshTokenExpirationCookie()).thenReturn(expiredCookie);

    mvc.perform(post("/api/users/me/withdraw")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(request)))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME, 0));

    verify(userService).withdraw(id, PASSWORD);
  }

  @Test
  @DisplayName("소셜 계정 탈퇴 인증 URL 준비")
  void prepareOAuthWithdrawal() throws Exception {
    authenticate();
    when(oauthWithdrawalService.prepare(id)).thenReturn(
        new OAuthWithdrawalAuthorizationResponse(
            "/oauth2/authorization/google?withdrawalRequestId=request-id"
        )
    );

    mvc.perform(post("/api/users/me/withdrawal/oauth2/authorization"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authorizationUrl").value(
            "/oauth2/authorization/google?withdrawalRequestId=request-id"
        ));

    verify(oauthWithdrawalService).prepare(id);
  }

  @Test
  @DisplayName("미 존재 유저 조회시 실패.")
  void missingUserResponse() throws Exception {
    when(userService.getUser(id)).thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

    mvc.perform(get("/api/users/{id}", id)).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
  }

  @Test
  @DisplayName("유저 조회시 UUID 포맷이 아닐 경우 400.")
  void invalidId() throws Exception {
    mvc.perform(get("/api/users/not-a-uuid")).andExpect(status().isBadRequest());

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("이미지 포함해서 수정")
  void updateWithImage() throws Exception {
    authenticate();

    MockMultipartFile image =
        new MockMultipartFile("image", "profile.png", "image/png", new byte[]{1});

    when(userService.updateUser(eq(id), eq(id), eq(updateRequest), any())).thenReturn(dto);

    mvc.perform(multipart(HttpMethod.PATCH, "/api/users/{id}", id).file(image)
            .file(new MockMultipartFile("request", "", "application/json",
                json.writeValueAsBytes(updateRequest))))
        .andExpect(status().isOk());

    verify(userService).updateUser(eq(id), eq(id), eq(updateRequest), argThat(file ->
        file.getOriginalFilename().equals("profile.png") && file.getSize() == 1));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 50, 51})
  @DisplayName("프로필 이름은 notblank, size(max 50)")
  void profileNameBoundary(int length) throws Exception {
    authenticate();

    UserProfileUpdateRequest request = new UserProfileUpdateRequest("a".repeat(length));

    if (length == 50) {
      when(userService.updateUser(id, id, request, null)).thenReturn(dto);
    }

    ResultActions response = mvc.perform(multipart(HttpMethod.PATCH, "/api/users/{id}", id)
        .file(new MockMultipartFile("request", "", "application/json",
            json.writeValueAsBytes(request))));

    if (length == 50) {
      response.andExpect(status().isOk());
      verify(userService).updateUser(id, id, request, null);
    } else {
      response.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.name").exists());
      verifyNoInteractions(userService);
    }
  }

  @Test
  void missingMultipartRequest() throws Exception {
    mvc.perform(multipart(HttpMethod.PATCH, "/api/users/{id}", id))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("관리자가 탈퇴 사용자를 조기 영구 삭제한다")
  void purgeWithdrawnUser() throws Exception {
    authenticate();

    mvc.perform(delete("/api/users/{userId}/purge", id))
        .andExpect(status().isNoContent());

    verify(userService).purgeUser(id, id);
  }

  private void authenticate() {
    UserDetails principal = new UserDetails(dto, "hash");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }
}
