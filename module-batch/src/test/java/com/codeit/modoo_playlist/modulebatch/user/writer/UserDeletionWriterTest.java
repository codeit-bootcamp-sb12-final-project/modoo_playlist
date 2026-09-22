package com.codeit.modoo_playlist.modulebatch.user.writer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.modulebatch.user.model.UserDeletionTarget;
import com.codeit.modoo_playlist.modulebatch.user.persistence.UserDeletionMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;

@ExtendWith(MockitoExtension.class)
class UserDeletionWriterTest {

  private static final Instant CUTOFF = Instant.parse("2026-09-21T15:00:00Z");
  private static final String USER_ID = "01999999-9999-7999-8999-999999999999";

  @Mock
  private UserDeletionMapper mapper;

  @Test
  void deletesRestrictedDataBeforeUser() throws Exception {
    when(mapper.lockDeletionTarget(USER_ID, CUTOFF)).thenReturn(USER_ID);
    when(mapper.deleteUser(USER_ID, CUTOFF)).thenReturn(1);

    new UserDeletionWriter(mapper, CUTOFF)
        .write(new Chunk<>(List.of(new UserDeletionTarget(USER_ID))));

    InOrder order = inOrder(mapper);
    order.verify(mapper).lockDeletionTarget(USER_ID, CUTOFF);
    order.verify(mapper).deleteMessages(USER_ID);
    order.verify(mapper).deleteWatchingSessions(USER_ID);
    order.verify(mapper).deleteReviews(USER_ID);
    order.verify(mapper).deleteUser(USER_ID, CUTOFF);
    verifyNoMoreInteractions(mapper);
  }

  @Test
  void skipsUserThatIsNoLongerEligible() throws Exception {
    when(mapper.lockDeletionTarget(USER_ID, CUTOFF)).thenReturn(null);

    new UserDeletionWriter(mapper, CUTOFF)
        .write(new Chunk<>(List.of(new UserDeletionTarget(USER_ID))));

    verify(mapper).lockDeletionTarget(USER_ID, CUTOFF);
    verifyNoMoreInteractions(mapper);
  }

  @Test
  void failsChunkWhenUserRowWasNotDeleted() {
    when(mapper.lockDeletionTarget(USER_ID, CUTOFF)).thenReturn(USER_ID);
    when(mapper.deleteUser(USER_ID, CUTOFF)).thenReturn(0);

    UserDeletionWriter writer = new UserDeletionWriter(mapper, CUTOFF);

    assertThatThrownBy(
        () -> writer.write(new Chunk<>(List.of(new UserDeletionTarget(USER_ID))))
    ).isInstanceOf(IllegalStateException.class);
  }
}
