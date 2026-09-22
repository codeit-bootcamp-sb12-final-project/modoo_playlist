package com.codeit.modoo_playlist.modulerealtime.handler;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.core.global.exception.ErrorResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompConversionException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketStompErrorHandler extends StompSubProtocolErrorHandler {

    private final JsonMapper jsonMapper;

    @Override
    protected Message<byte[]> handleInternal(
            StompHeaderAccessor errorHeaders,
            byte[] errorPayload,
            @Nullable Throwable cause,
            @Nullable StompHeaderAccessor clientHeaders
    ) {
        if (cause == null) {
            return super.handleInternal(
                    errorHeaders, errorPayload, null, clientHeaders
            );
        }
        Throwable actual = unwrap(cause);
        BaseException exception = toBaseException(actual);
        ErrorCode code = exception.getErrorCode();

        if (code.getStatus() >= 500) {
            log.error("STOMP 처리 실패: code={}, exceptionType={}",
                    code,
                    actual.getClass().getName());
        }

        errorHeaders.setMessage(code.name());
        errorHeaders.setContentType(MimeTypeUtils.APPLICATION_JSON);

        byte[] body;
        try {
            body = jsonMapper.writeValueAsBytes(
                    new ErrorResponse(exception, code.getStatus())
            );
        } catch (JacksonException serializationError) {
            log.error("STOMP 오류 응답 직렬화 실해: exceptionType={}",
                    serializationError.getClass().getName());

            errorHeaders.setMessage(ErrorCode.INTERNAL_SERVER_ERROR.name());
            body = """
                    {
                      "code": "INTERNAL_SERVER_ERROR",
                      "message": "서버 내부 오류가 발생했습니다.",
                      "status": 500
                    }
                    """.getBytes(StandardCharsets.UTF_8);
        }

        return MessageBuilder.createMessage(
                body, errorHeaders.getMessageHeaders());
    }

    private Throwable unwrap(Throwable exception) {
        while (exception instanceof MessagingException
                && exception.getCause() != null
                && exception.getCause() != exception) {
            exception = exception.getCause();
        }
        return exception;
    }

    private BaseException toBaseException(Throwable exception){
        if(exception instanceof BaseException baseException){
            return baseException;
        }

        if(exception instanceof AccessDeniedException) {
            return new BaseException(ErrorCode.ACCESS_DENIED);
        }

        if(exception instanceof StompConversionException) {
            return new BaseException(ErrorCode.INVALID_REQUEST);
        }

        return new BaseException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

}
