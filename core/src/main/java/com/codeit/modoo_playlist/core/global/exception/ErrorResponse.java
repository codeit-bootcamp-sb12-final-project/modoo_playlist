package com.codeit.modoo_playlist.core.global.exception;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// GlobalExceptionHandler에서 에러 응답 바디로 사용
// timestamp, code, message, details, exceptionType, status 포함한 구조화된 에러 응답
@Getter
@RequiredArgsConstructor
public class ErrorResponse {
    private final Instant timestamp;
    private final String code;
    private final String message;
    private final Map<String, Object> details;  // 필드별 상세 오류 정보
    private final String exceptionType;         // 예외 클래스명
    private final int status;                   // HTTP 상태 코드

    // BaseException (커스텀 예외) 로부터 생성
    public ErrorResponse(BaseException exception, int status) {
        this(
            Instant.now(),
            exception.getErrorCode().name(),
            exception.getMessage(),
            exception.getDetails(),
            exception.getClass().getSimpleName(),
            status
        );
    }

    // 일반 Exception 으로부터 생성
    public ErrorResponse(Exception exception, int status) {
        this(
            Instant.now(),
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            new HashMap<>(),
            exception.getClass().getSimpleName(),
            status
        );
    }
}
