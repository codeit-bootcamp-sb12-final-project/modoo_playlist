package com.codeit.modoo_playlist.modulerealtime.sse;

import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.modulerealtime.security.RealtimePrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sse")
public class SseController {

    private final SseService sseService;

    @PreAuthorize("hasRole('USER')")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal RealtimePrincipal user,
            @RequestParam(value = "LastEventId", required = false) String lastEventId
    ) {
        if(user == null){
            throw new BaseException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        UUID last = null;

        if(lastEventId != null && !lastEventId.isBlank()){
            last = UUID.fromString(lastEventId);
        }
        return sseService.connect(user.userId(), last);
    }

}
