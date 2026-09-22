package com.codeit.modoo_playlist.modulebatch.user.persistence;

import com.codeit.modoo_playlist.modulebatch.user.model.UserDeletionTarget;
import java.time.Instant;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.jspecify.annotations.Nullable;

@Mapper
public interface UserDeletionMapper {

  List<UserDeletionTarget> findDeletionTargets(
      @Param("cutoff") Instant cutoff,
      @Param("lastUserId") @Nullable String lastUserId,
      @Param("limit") int limit
  );

  @Nullable String lockDeletionTarget(
      @Param("userId") String userId,
      @Param("cutoff") Instant cutoff
  );

  int deleteMessages(@Param("userId") String userId);

  int deleteWatchingSessions(@Param("userId") String userId);

  int deleteReviews(@Param("userId") String userId);

  int deleteUser(
      @Param("userId") String userId,
      @Param("cutoff") Instant cutoff
  );
}
