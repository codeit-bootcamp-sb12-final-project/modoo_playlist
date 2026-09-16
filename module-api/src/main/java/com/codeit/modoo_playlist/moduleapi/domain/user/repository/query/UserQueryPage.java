package com.codeit.modoo_playlist.moduleapi.domain.user.repository.query;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import java.util.List;
import java.util.UUID;

public record UserQueryPage(
    List<User> users,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount
) {

  public UserQueryPage {
    users = List.copyOf(users);
  }
}