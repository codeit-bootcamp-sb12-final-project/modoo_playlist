package com.codeit.modoo_playlist.modulebatch.user.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.modulebatch.user.model.UserDeletionTarget;
import com.codeit.modoo_playlist.modulebatch.user.persistence.UserDeletionMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDeletionReaderTest {

  private static final Instant CUTOFF = Instant.parse("2026-09-21T15:00:00Z");

  @Mock
  private UserDeletionMapper mapper;

  @Test
  void readsTargetsWithKeysetPagination() throws Exception {
    UserDeletionTarget first = new UserDeletionTarget("user-1");
    UserDeletionTarget second = new UserDeletionTarget("user-2");
    when(mapper.findDeletionTargets(CUTOFF, null, 2)).thenReturn(List.of(first, second));
    when(mapper.findDeletionTargets(CUTOFF, "user-2", 2)).thenReturn(List.of());

    UserDeletionReader reader = new UserDeletionReader(mapper, CUTOFF, 2, 10);

    assertThat(reader.read()).isEqualTo(first);
    assertThat(reader.read()).isEqualTo(second);
    assertThat(reader.read()).isNull();
    verify(mapper).findDeletionTargets(CUTOFF, "user-2", 2);
  }

  @Test
  void stopsAtMaxItemsWithoutFetchingAnotherPage() throws Exception {
    when(mapper.findDeletionTargets(CUTOFF, null, 2)).thenReturn(List.of(
        new UserDeletionTarget("user-1"),
        new UserDeletionTarget("user-2")
    ));

    UserDeletionReader reader = new UserDeletionReader(mapper, CUTOFF, 2, 1);

    assertThat(reader.read()).isNotNull();
    assertThat(reader.read()).isNull();
    verify(mapper, never()).findDeletionTargets(CUTOFF, "user-2", 2);
  }
}
