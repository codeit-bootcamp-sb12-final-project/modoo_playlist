package com.codeit.modoo_playlist.moduleapi.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    @Value("${sse.timeout:1800000}") // 기본 30분
    private long timeout;

    private final SseEmitterRepository sseEmitterRepository;
    private final SseMessageRepository sseMessageRepository;

    // 연결 시도
    public SseEmitter connect(UUID receiverId, UUID lastEventId) {
        SseEmitter emitter = new SseEmitter(timeout);

        emitter.onCompletion(() -> sseEmitterRepository.delete(receiverId, emitter));
        emitter.onTimeout(() -> sseEmitterRepository.delete(receiverId, emitter));
        emitter.onError(ex -> sseEmitterRepository.delete(receiverId, emitter));

        sseEmitterRepository.save(receiverId, emitter);

        // 마지막 아이디를 기준으로 다시 이벤트를 살리는 코드
        // -> client에서 에러가 발생하여 기존 이벤트를 못받았을때 다시 요청하는 코드
        Optional.ofNullable(lastEventId)
                .ifPresentOrElse(
                        id -> sseMessageRepository.findAllByEventIdAfterAndReceiverId(id, receiverId)
                                .forEach(msg -> {
                                    try {
                                        emitter.send(
                                                SseEmitter.event()
                                                        .id(msg.getEventId().toString())
                                                        .name(msg.getEventName())
                                                        .data(msg.getData())
                                        );
                                    } catch (Exception e) {
                                        log.error("Failed to send message: {}", e.getMessage());
                                        emitter.completeWithError(e);
                                    }
                                }),
                        () -> ping(emitter)
                );

        return emitter;
    }

    // 특정 유저들에게만 알림 (DM, 채널 알림에 사용)
    public void send(Collection<UUID> receiverIds, String eventName, Object data) {
        SseMessage message = sseMessageRepository.save(
                SseMessage.create(receiverIds, eventName, data));

        List<SseEmitter> emitters = sseEmitterRepository.findAllByReceiverIdsIn(receiverIds);

        log.debug("SSE send. eventName={} receivers={} emitterCount={}",
                eventName, receiverIds, emitters.size());

        emitters.forEach(emitter -> {
            try {
                emitter.send(
                        SseEmitter.event()
                                .id(message.getEventId().toString())
                                .name(eventName)
                                .data(data)
                );
            } catch (Exception e) {
                log.error("Failed to send message: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }

    // 전체 브로드캐스트 (필요하면 사용)
    public void broadcast(String eventName, Object data) {
        log.debug("SSE broadcast. eventName={}", eventName);
        SseMessage message = sseMessageRepository.save(
                SseMessage.createBroadcast(eventName, data));
        Set<ResponseBodyEmitter.DataWithMediaType> events = message.toEvent();
        sseEmitterRepository.findAll().forEach(emitter -> {
            try {
                emitter.send(events);
            } catch (Exception e) {
                log.error("Failed to send message: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }

    // 오래된 emitter 정리 + ping
    @Scheduled(fixedDelay = 15000L)
    public void cleanUp() {
        sseEmitterRepository.findAll().stream()
                .filter(emitter -> !ping(emitter))
                .forEach(SseEmitter::complete);
    }

    private boolean ping(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("ping").build());
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("Failed to send ping event: {}", e.getMessage());
            return false;
        }
    }
}
