package com.codeit.modoo_playlist.infra.store;

import java.time.Duration;
import java.util.List;

// 순서가 있는 목록 저장소 추상화 (사용자별 활성 세션 큐 등에 사용)
public interface ListStore<K, V> {

    void pushRight(K key, V value);

    V popLeft(K key);

    void remove(K key, V value);

    List<V> range(K key);

    long size(K key);

    void expire(K key, Duration ttl);

    void delete(K key);
}
