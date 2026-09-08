package com.codeit.modoo_playlist.core.global.common.dto.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class SlicePageResponse<T> {
    int size;
    boolean hasNext;
    Object nextCursor;
    List<T> items;
}
