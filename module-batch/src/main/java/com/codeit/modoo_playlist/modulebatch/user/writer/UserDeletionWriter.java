package com.codeit.modoo_playlist.modulebatch.user.writer;

import com.codeit.modoo_playlist.modulebatch.user.model.UserDeletionTarget;
import com.codeit.modoo_playlist.modulebatch.user.persistence.UserDeletionMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

@Slf4j
@RequiredArgsConstructor
public class UserDeletionWriter implements ItemWriter<UserDeletionTarget> {

  private final UserDeletionMapper mapper;
  private final Instant cutoff;

  @Override
  public void write(Chunk<? extends UserDeletionTarget> chunk) {
    int deletedCount = 0;
    for (UserDeletionTarget target : chunk) {
      String userId = target.userId();
      if (mapper.lockDeletionTarget(userId, cutoff) == null) {
        log.info("사용자 영구 삭제 대상에서 제외되었습니다: userId={}", userId);
        continue;
      }

      mapper.deleteMessages(userId);
      mapper.deleteWatchingSessions(userId);
      mapper.deleteReviews(userId);

      if (mapper.deleteUser(userId, cutoff) != 1) {
        throw new IllegalStateException("사용자 영구 삭제에 실패했습니다: userId=" + userId);
      }
      log.info("탈퇴 사용자 영구 삭제 완료: userId={}", userId);
      deletedCount++;
    }

    log.info("탈퇴 사용자 영구 삭제: {}건", deletedCount);
  }
}
