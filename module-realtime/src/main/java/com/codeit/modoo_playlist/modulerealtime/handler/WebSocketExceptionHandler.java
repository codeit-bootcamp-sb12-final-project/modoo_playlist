package com.codeit.modoo_playlist.modulerealtime.handler;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorResponse;
import com.codeit.modoo_playlist.modulerealtime.message.controller.MessageController;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice(basePackageClasses = MessageController.class)
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(BaseException.class)
    @SendToUser(destinations = "/sub/errors", broadcast = false)
    public ErrorResponse handleBaseException(BaseException exception) {
        return new ErrorResponse(exception, exception.getErrorCode().getStatus());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(destinations = "/sub/errors", broadcast = false)
    public ErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (exception.getBindingResult() != null) {
            exception.getBindingResult().getAllErrors().forEach(error -> {
                String field = error instanceof FieldError fieldError
                        ? fieldError.getField() : error.getObjectName();
                details.put(field, error.getDefaultMessage());
            });
        }
        return new ErrorResponse(
                Instant.now(), "VALIDATION_ERROR", "요청 데이터 유효성 검사에 실패하였습니다.",
                details, exception.getClass().getSimpleName(), 400);
    }
}
