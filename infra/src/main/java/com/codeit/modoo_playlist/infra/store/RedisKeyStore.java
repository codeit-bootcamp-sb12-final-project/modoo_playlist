package com.codeit.modoo_playlist.infra.store;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

// Redis String 자료구조를 이용한 KeyStore 구현체.
public class RedisKeyStore<K, V> implements KeyStore<K, V> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String prefix;

    public RedisKeyStore(RedisTemplate<String, Object> redisTemplate, String prefix) {
        this.redisTemplate = redisTemplate;
        this.prefix = prefix;
    }

    private String k(K key) {
        return prefix + ":" + key;
    }

    // TTL이 있으면 만료시간과 함께, 없으면 영구 저장한다.
    @Override
    public void put(K key, V value, Duration ttl) {
        if (ttl == null) {
            redisTemplate.opsForValue().set(k(key), value);
        } else {
            redisTemplate.opsForValue().set(k(key), value, ttl);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<V> get(K key) {
        Object value = redisTemplate.opsForValue().get(k(key));
        return Optional.ofNullable((V) value);
    }

    @Override
    public boolean delete(K key) {
        return redisTemplate.delete(k(key));
    }

    @Override
    public boolean exists(K key) {
        return redisTemplate.hasKey(k(key));
    }
}
