package com.codeit.modoo_playlist.infra.store;

import java.time.Duration;
import java.util.Optional;

// 단일 키-값 저장소 추상화
public interface KeyStore<K, V> {

    void put(K key, V value, Duration ttl);

    Optional<V> get(K key);

    boolean delete(K key);

    boolean exists(K key);
}
