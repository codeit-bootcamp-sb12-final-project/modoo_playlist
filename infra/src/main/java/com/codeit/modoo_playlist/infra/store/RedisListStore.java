package com.codeit.modoo_playlist.infra.store;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Redis List 자료구조를 이용한 ListStore 구현체.
public class RedisListStore<K, V> implements ListStore<K, V> {

    private final RedisTemplate<String, Object> redisTemplate;
    private final String prefix;

    public RedisListStore(RedisTemplate<String, Object> redisTemplate, String prefix) {
        this.redisTemplate = redisTemplate;
        this.prefix = prefix;
    }

    private String k(K key) {
        return prefix + ":" + key;
    }

    @Override
    public void pushRight(K key, V value) {
        redisTemplate.opsForList().rightPush(k(key), value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V popLeft(K key) {
        return (V) redisTemplate.opsForList().leftPop(k(key));
    }

    @Override
    public void remove(K key, V value) {
        redisTemplate.opsForList().remove(k(key), 0, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<V> range(K key) {
        List<Object> values = redisTemplate.opsForList().range(k(key), 0, -1);
        if (values == null) return Collections.emptyList();
        return values.stream().map(v -> (V) v).filter(Objects::nonNull).toList();
    }

    @Override
    public long size(K key) {
        Long size = redisTemplate.opsForList().size(k(key));
        return size == null ? 0 : size;
    }

    @Override
    public void expire(K key, Duration ttl) {
        redisTemplate.expire(k(key), ttl);
    }

    @Override
    public void delete(K key) {
        redisTemplate.delete(k(key));
    }
}
