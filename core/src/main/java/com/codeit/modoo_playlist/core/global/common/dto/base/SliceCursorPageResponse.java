package com.codeit.modoo_playlist.core.global.common.dto.base;

import com.querydsl.core.types.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class SliceCursorPageResponse<T> {
    private String nextCursor;  // 다음 페이지 커서 (id 등)
    private Instant nextAfter;  // 다음 페이지 기준 시각 (createdAt 등)
    private int size;
    private long totalElements;
    private boolean hasNext;
    private String sort;                // 정렬 기준 컬럼명
    private Order direction;            // 정렬 방향 (ASC/DESC)
    private List<T> content;
}
