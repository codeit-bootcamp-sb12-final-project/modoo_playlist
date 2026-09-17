package com.codeit.modoo_playlist.moduleapi.exception;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.core.global.exception.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ErrorResponse> handleBaseException(BaseException e) {
    log.error("커스텀 예외 발생 : code={}, message={}, detail={}", e.getErrorCode(), e.getMessage(),
        e.getDetails());

    int status = e.getErrorCode().getStatus();

    return ResponseEntity
        .status(HttpStatus.valueOf(status))
        .body(new ErrorResponse(e, status));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
    return handleBaseException(new BaseException(ErrorCode.ACCESS_DENIED, e));
  }

  // 클라이언트가 이미 연결을 끊은 상태(SSE 등 비동기 응답 도중 탭 닫힘/새로고침) — 응답 쓸 대상이 없으니 무시
  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
    log.debug("비동기 요청 처리 중 클라이언트 연결이 이미 끊어짐: {}", e.getMessage());
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

  // 400 - @RequestPart @Valid 실패 (multipart) → 필드별 에러맵 반환
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
    log.error("요청 유효성 검사 실패(multipart) : {}", e.getMessage());

    Map<String, Object> details = new LinkedHashMap<>();
    e.getConstraintViolations().forEach(cv -> {
      // 경로에서 마지막 필드명만 추출 (ex. create.user.username -> username)
      String path = cv.getPropertyPath().toString();
      String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
      details.put(field, cv.getMessage());
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
    ErrorCode errorCode = ErrorCode.CONFLICT;
    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        errorCode.name(),
        errorCode.getMessage(),
        new LinkedHashMap<>(),
        e.getClass().getSimpleName(),
        errorCode.getStatus()
    );
    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  // 413 - 파일 크기 초과
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException e) {
    ErrorCode errorCode = ErrorCode.PAYLOAD_TOO_LARGE;
    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        errorCode.name(),
        errorCode.getMessage(),
        new LinkedHashMap<>(),
        e.getClass().getSimpleName(),
        errorCode.getStatus()
    );
    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }

  // 500 - 그 외 모든 예외
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("예상치 못한 오류 발생 : {}", e.getMessage(), e);
    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    ErrorResponse response = new ErrorResponse(
        Instant.now(),
        errorCode.name(),
        errorCode.getMessage(),
        new LinkedHashMap<>(),
        e.getClass().getSimpleName(),
        errorCode.getStatus()
    );
    return ResponseEntity.status(errorCode.getStatus()).body(response);
  }
}
