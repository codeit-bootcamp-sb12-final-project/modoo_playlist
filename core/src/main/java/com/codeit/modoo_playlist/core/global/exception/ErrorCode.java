package com.codeit.modoo_playlist.core.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // Social
  // 추후 추가

  // Auth
  INVALID_CREDENTIALS(401, "아이디 또는 비밀번호가 올바르지 않습니다."),
  AUTHENTICATION_REQUIRED(401, "로그인이 필요합니다."),
  ACCESS_DENIED(403, "접근 권한이 없습니다."),
  INVALID_TOKEN(401, "유효하지 않은 인증 토큰입니다."),
  ACCESS_TOKEN_EXPIRED(401, "액세스 토큰이 만료되었습니다,"),
  REFRESH_TOKEN_EXPIRED(401, "로그인 유지 기간이 만료되었습니다. 다시 로그인해 주세요."),
  LOGIN_SESSION_INVALIDATED(401, "로그인이 만료되었거나 해제되었습니다."),
  INVALID_CSRF_TOKEN(403, "요청 검증에 실패했습니다."),

  // User
  USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
  EMAIL_ALREADY_EXISTS(409, "이미 사용 중인 이메일입니다."),
  USER_ALREADY_EXISTS(409, "이미 존재하는 사용자입니다."),
  USER_ACCOUNT_LOCKED(403, "잠긴 계정입니다."),

  // Post
  POST_NOT_FOUND(404, "게시글을 찾을 수 없습니다."),

  // Comment
  COMMENT_NOT_FOUND(404, "댓글을 찾을 수 없습니다."),

  // File
  FILE_SAVE_FAILED(500, "파일 저장에 실패했습니다."),
  FILE_DELETE_FAILED(500, "파일 삭제에 실패했습니다."),
  PAYLOAD_TOO_LARGE(413, "업로드 가능한 파일 크기를 초과했습니다."),

  // Common
  INVALID_REQUEST(400, "잘못된 요청입니다."),
  CONFLICT(409, "이미 존재하는 데이터입니다."),
  INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

  private final int status;
  private final String message;
}
