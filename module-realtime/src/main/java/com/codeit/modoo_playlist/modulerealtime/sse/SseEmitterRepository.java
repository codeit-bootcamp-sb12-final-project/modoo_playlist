package com.codeit.modoo_playlist.modulerealtime.sse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SseEmitterRepository {

    private static final long DEFAULT_TIMEOUT = 30L * 60 * 1000;

    private final Map<UUID, List<SseEmitter>> data = new ConcurrentHashMap<>();

    public SseEmitter save(UUID receiverId, SseEmitter emitter){
        data.compute(receiverId, (key,emitters) ->{
            if(emitters == null) {
                return new CopyOnWriteArrayList<>(List.of(emitter));
            } else {
                emitters.add(emitter);
                return emitters;
            }
        });
        return emitter;
    }

    public Optional<List<SseEmitter>> findByReceiverId(UUID receiverId) {
        return Optional.ofNullable(data.get(receiverId));
    }

    public List<SseEmitter> findAllByReceiverIdsIn(Collection<UUID> receiverIds) {
        return data.entrySet().stream()
                .filter(entry -> receiverIds.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .toList();
    }

    public List<SseEmitter> findAll() {
        return data.values().stream()
                .flatMap(Collection::stream)
                .toList();
    }

    public void delete(UUID receiverId, SseEmitter emitter) {
        data.computeIfPresent(receiverId, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

}
