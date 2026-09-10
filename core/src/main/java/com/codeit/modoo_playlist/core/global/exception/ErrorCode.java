package com.codeit.modoo_playlist.core.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS("이미 존재하는 사용자입니다."),
    INVALID_CREDENTIALS("아이디 또는 비밀번호가 올바르지 않습니다."),

    // Post
    POST_NOT_FOUND("게시글을 찾을 수 없습니다."),

    // Comment
    COMMENT_NOT_FOUND("댓글을 찾을 수 없습니다."),

    // Playlist
    PLAYLIST_NOT_FOUND("플레이리스트를 찾을 수 없습니다."),
    PLAYLIST_ACCESS_DENIED("본인의 플레이리스트가 아닙니다."),
    PLAYLIST_CONTENT_NOT_FOUND("플레이리스트에 등록된 콘텐츠를 찾을 수 없습니다."),
    PLAYLIST_CONTENT_ALREADY_EXISTS("이미 플레이리스트에 등록된 콘텐츠입니다."),
    PLAYLIST_SUBSCRIPTION_NOT_FOUND("구독 관계를 찾을 수 없습니다."),
    PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS("이미 구독 중인 플레이리스트입니다."),

    // Conversation
    CONVERSATION_NOT_FOUND("대화를 찾을 수 없습니다."),
    CONVERSATION_ACCESS_DENIED("해당 대화에 접근할 권한이 없습니다."),

    // File
    FILE_SAVE_FAILED("파일 저장에 실패했습니다."),
    FILE_DELETE_FAILED("파일 삭제에 실패했습니다."),

    // Common
    INVALID_REQUEST("잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.");

    private final String message;
}