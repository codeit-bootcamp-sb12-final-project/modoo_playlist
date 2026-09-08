package com.codeit.modoo_playlist.core.global.common.dto.base;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값은 응답에 포함하지 않음
@Schema(description = "공통 API 응답 객체")
public class ApiResult<T> {

    @Schema(description = "요청 성공 여부", example = "true")
    private boolean success;
    @Schema(description = "응답 메시지", example = "정상적으로 처리되었습니다.")
    private String message;  // 응답 메시지

    @Schema(description = "응답 데이터 ", nullable = true)
    private T data;

    @Schema(description = "에러 정보", nullable = true, implementation = ApiError.class)
    private ApiError error;

    @Schema(description = "응답 생성 시각 (UTC)", example = "2025-08-18T08:30:45.123Z")
    private Instant timestamp;

    public static <T> ApiResult<T> ok(T data) {
        return ApiResult.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return ApiResult.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResult<T> fail(String code, String message) {
        return ApiResult.<T>builder()
                .success(false)
                .error(new ApiError(code, message))
                .timestamp(Instant.now())
                .build();
    }
}