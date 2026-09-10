package com.codeit.modoo_playlist.moduleapi.exception;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.core.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
        log.error("커스텀 예외 발생 : code={}, message={}, detail={}", e.getErrorCode(), e.getMessage(), e.getDetails());
        HttpStatus status = parseHttpStatus(e);
        ErrorResponse response = new ErrorResponse(e, status.value());
        return ResponseEntity.status(status).body(response);
    }

    private HttpStatus parseHttpStatus(BaseException e) {
        ErrorCode code = e.getErrorCode();
        return switch (code) {
            case USER_NOT_FOUND, POST_NOT_FOUND, COMMENT_NOT_FOUND, FOLLOW_NOT_FOUND, CONVERSATION_NOT_FOUND,
                 PLAYLIST_NOT_FOUND, PLAYLIST_CONTENT_NOT_FOUND, PLAYLIST_SUBSCRIPTION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case USER_ALREADY_EXISTS, FOLLOW_ALREADY_EXISTS,
                 PLAYLIST_CONTENT_ALREADY_EXISTS, PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case PLAYLIST_ACCESS_DENIED, CONVERSATION_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case INVALID_REQUEST, SELF_FOLLOW_NOT_ALLOWED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    // 400 - @RequestBody @Valid 실패 → 필드별 에러맵 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        log.error("요청 유효성 검사 실패 : {}", e.getMessage());

        Map<String, Object> details = new LinkedHashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            details.put(field, error.getDefaultMessage());
        });

        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                "VALIDATION_ERROR",
                "요청 데이터 유효성 검사에 실패하였습니다.",
                details,
                e.getClass().getSimpleName(),
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

//    // 400 - @RequestPart @Valid 실패 (multipart) → 필드별 에러맵 반환
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
//        log.error("요청 유효성 검사 실패(multipart) : {}", e.getMessage());
//
//        Map<String, Object> details = new LinkedHashMap<>();
//        e.getConstraintViolations().forEach(cv -> {
//            // 경로에서 마지막 필드명만 추출 (ex. create.user.username -> username)
//            String path = cv.getPropertyPath().toString();
//            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
//            details.put(field, cv.getMessage());
//        });
//
//        ErrorResponse response = new ErrorResponse(
//                Instant.now(),
//                "VALIDATION_ERROR",
//                "요청 데이터 유효성 검사에 실패하였습니다.",
//                details,
//                e.getClass().getSimpleName(),
//                HttpStatus.BAD_REQUEST.value()
//        );
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }

    // 404 - 데이터 없음
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e) {
        ErrorResponse response = new ErrorResponse(e, HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 400 - 잘못된 요청 (파라미터 오류, 파트 누락, 타입 불일치)
    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        ErrorResponse response = new ErrorResponse(e, HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 409 - 중복 데이터 (username, email unique 제약 위반)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                "CONFLICT",
                "이미 존재하는 데이터입니다.",
                new LinkedHashMap<>(),
                e.getClass().getSimpleName(),
                HttpStatus.CONFLICT.value()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 413 - 파일 크기 초과
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException e) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                "PAYLOAD_TOO_LARGE",
                "업로드 가능한 파일 크기를 초과했습니다.",
                new LinkedHashMap<>(),
                e.getClass().getSimpleName(),
                HttpStatus.PAYLOAD_TOO_LARGE.value()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    // 500 - 파일 I/O 오류
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIO(IOException e) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                "IO_ERROR",
                "파일 처리 중 오류가 발생했습니다: " + e.getMessage(),
                new LinkedHashMap<>(),
                e.getClass().getSimpleName(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // 500 - RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException e, HttpServletRequest req) {
        log.error("예상치 못한 RuntimeException 발생 : {}", e.getMessage(), e);
        ErrorResponse response = new ErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // 500 - 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest req) {
        log.error("예상치 못한 오류 발생 : {}", e.getMessage(), e);
        ErrorResponse response = new ErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
