package com.codeit.modoo_playlist.core.global.common.dto.base;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SlicePageResponse<T> {
    int size;
    boolean hasNext;
    Object nextCursor;
    List<T> items;
}
