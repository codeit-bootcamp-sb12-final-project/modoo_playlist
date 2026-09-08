package com.codeit.modoo_playlist.core.global.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final Instant timestamp;
    private final ErrorCode errorCode;
    private final Map<String, Object> details; // 에러 상세 정보 (필드명, 값 등)

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.timestamp = Instant.now();
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    // 원인 예외 포함 생성자
    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.timestamp = Instant.now();
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }

    // 상세 정보 추가 (key-value)
    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }
}
