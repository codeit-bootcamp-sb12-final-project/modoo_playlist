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

  // Follow
  FOLLOW_NOT_FOUND(404, "팔로우 관계를 찾을 수 없습니다."),
  FOLLOW_ALREADY_EXISTS(409, "이미 팔로우하고 있습니다."),
  SELF_FOLLOW_NOT_ALLOWED(400, "자기 자신을 팔로우할 수 없습니다."),
  FOLLOW_ACCESS_DENIED(403, "본인의 팔로우 관계가 아닙니다."),

  // Review
  REVIEW_NOT_FOUND(404, "리뷰를 찾을 수 없습니다."),
  REVIEW_ALREADY_EXISTS(409, "이미 이 콘텐츠에 리뷰를 작성했습니다."),
  REVIEW_ACCESS_DENIED(403, "본인이 작성한 리뷰가 아닙니다."),

  // Playlist
  PLAYLIST_NOT_FOUND(404, "플레이리스트를 찾을 수 없습니다."),
  PLAYLIST_ACCESS_DENIED(403, "본인의 플레이리스트가 아닙니다."),
  PLAYLIST_CONTENT_NOT_FOUND(404, "플레이리스트에 등록된 콘텐츠를 찾을 수 없습니다."),
  PLAYLIST_CONTENT_ALREADY_EXISTS(409, "이미 플레이리스트에 등록된 콘텐츠입니다."),
  PLAYLIST_SUBSCRIPTION_NOT_FOUND(404, "구독 관계를 찾을 수 없습니다."),
  PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS(409, "이미 구독 중인 플레이리스트입니다."),

  // Conversation
  CONVERSATION_NOT_FOUND(404, "대화를 찾을 수 없습니다."),
  CONVERSATION_ACCESS_DENIED(403, "해당 대화에 접근할 권한이 없습니다."),

  // File
  FILE_SAVE_FAILED(500, "파일 저장에 실패했습니다."),
  FILE_DELETE_FAILED(500, "파일 삭제에 실패했습니다."),
  PAYLOAD_TOO_LARGE(413, "업로드 가능한 파일 크기를 초과했습니다."),

  // Content
  CONTENT_NOT_FOUND(404, "콘텐츠를 찾을 수 없습니다."),
  CONTENT_TYPE_INVALID(400, "지원하지 않는 콘텐츠 타입입니다."),
  CONTENT_SORT_INVALID(400, "지원하지 않는 콘텐츠 정렬 기준입니다."),
  CONTENT_SORT_DIRECTION_INVALID(400, "지원하지 않는 정렬 방향입니다."),
  CONTENT_QUERY_INVALID(400, "콘텐츠 조회 조건이 올바르지 않습니다."),
  CONTENT_CURSOR_INVALID(400, "콘텐츠 커서 형식이 올바르지 않습니다."),
  THUMBNAIL_INVALID(400, "올바른 썸네일 이미지가 아닙니다."),

  // Tag
  TAG_NAME_INVALID(400, "태그 이름이 올바르지 않습니다."),
  TAG_SYNC_FAILED(500, "태그 저장 결과를 확인하지 못했습니다."),

  // Batch - TMDB
  TMDB_CONTENT_INVALID(422, "TMDB 콘텐츠의 필수 정보가 올바르지 않습니다."),
  TMDB_ITEM_FETCH_FAILED(502, "TMDB 콘텐츠 상세 정보를 가져오지 못했습니다."),
  TMDB_AUTHENTICATION_FAILED(502, "TMDB 인증에 실패했습니다."),
  TMDB_PATH_UNAVAILABLE(503, "TMDB 목록 API를 사용할 수 없습니다."),

  // Batch - TheSportsDB
  SPORTSDB_EVENT_INVALID(422, "TheSportsDB 경기의 필수 정보가 올바르지 않습니다."),
  SPORTSDB_API_ERROR(502, "TheSportsDB API가 오류를 반환했습니다."),
  SPORTSDB_DISCOVERY_FAILED(503, "TheSportsDB 경기 목록을 가져오지 못했습니다."),
  SPORTSDB_AUTHENTICATION_FAILED(502, "TheSportsDB 인증에 실패했습니다."),

  // Common
  INVALID_REQUEST(400, "잘못된 요청입니다."),
  CONFLICT(409, "이미 존재하는 데이터입니다."),
  INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

  private final int status;
  private final String message;
}
