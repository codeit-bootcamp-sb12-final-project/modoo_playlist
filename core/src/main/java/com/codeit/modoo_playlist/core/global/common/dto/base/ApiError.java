package com.codeit.modoo_playlist.core.global.common.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "에러 정보")
public class ApiError {
    @Schema(description = "에러 코드", example = "USER_NOT_FOUND")
    private String code;                  // 예: NOT_FOUND, BAD_REQUEST, INTERNAL_ERROR
    @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다.")
    private String message;               // 사용자에게 보여줄 메시지
}