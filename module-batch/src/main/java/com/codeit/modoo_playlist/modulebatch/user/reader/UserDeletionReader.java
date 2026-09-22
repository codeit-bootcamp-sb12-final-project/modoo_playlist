package com.codeit.modoo_playlist.modulebatch.user.reader;

import com.codeit.modoo_playlist.modulebatch.user.model.UserDeletionTarget;
import com.codeit.modoo_playlist.modulebatch.user.persistence.UserDeletionMapper;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemReader;

public class UserDeletionReader implements ItemReader<UserDeletionTarget> {

  private final UserDeletionMapper mapper;
  private final Instant cutoff;
  private final int pageSize;
  private final int maxItems;

  private int emitted;
  private @Nullable String lastUserId;
  private Iterator<UserDeletionTarget> currentPage = List.<UserDeletionTarget>of().iterator();

  public UserDeletionReader(
      UserDeletionMapper mapper,
      Instant cutoff,
      int pageSize,
      int maxItems
  ) {
    this.mapper = mapper;
    this.cutoff = cutoff;
    this.pageSize = pageSize;
    this.maxItems = maxItems;
  }

  @Override
  public @Nullable UserDeletionTarget read() {
    if (emitted >= maxItems) {
      return null;
    }

    if (!currentPage.hasNext()) {
      List<UserDeletionTarget> page = mapper.findDeletionTargets(cutoff, lastUserId, pageSize);
      if (page.isEmpty()) {
        return null;
      }
      currentPage = page.iterator();
      lastUserId = page.get(page.size() - 1).userId();
    }

    emitted++;
    return currentPage.next();
  }
}
