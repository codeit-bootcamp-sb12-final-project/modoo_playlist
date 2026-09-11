package com.codeit.modoo_playlist.moduleapi.dto.response;

import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import java.util.List;
import java.util.UUID;

//현재 core의 페이지네이션 dto를 그대로 쓰기 힘들다고 판단.
//우선 UserDto 전용 dto를 만들고, 추후 수정해서 공용으로 변경 가능.
public record CursorResponseUserDto(
    List<UserDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {

}
