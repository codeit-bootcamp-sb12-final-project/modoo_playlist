package com.codeit.modoo_playlist.moduleapi.domain.user.repository.query;

import com.codeit.modoo_playlist.moduleapi.dto.user.request.UserListRequest;

public interface UserQueryRepository {

  UserQueryPage findAllUsers(UserListRequest request);
}
