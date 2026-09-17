package com.codeit.modoo_playlist.moduleapi.domain.interaction.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.interaction.entity.UserContentInteraction;
import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.repository.UserContentInteractionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReactionServiceImplTest {

  @Mock
  private UserContentInteractionRepository interactionRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private ContentRepository contentRepository;
  @InjectMocks
  private ReactionServiceImpl reactionService;

  @Test
  void 반응_저장_전에_사용자_행을_잠근다() {
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    User user = mock(User.class);
    Content content = mock(Content.class);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
    when(interactionRepository
        .findAllByUserIdAndContentIdAndTypeInOrderByUpdatedAtDescIdDesc(
            eq(userId), eq(contentId), any()))
        .thenReturn(List.of());
    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

    reactionService.setReaction(userId, contentId, InteractionType.LIKE);

    InOrder order = inOrder(userRepository, interactionRepository);
    order.verify(userRepository).findByIdForUpdate(userId);
    order.verify(interactionRepository)
        .findAllByUserIdAndContentIdAndTypeInOrderByUpdatedAtDescIdDesc(
            eq(userId), eq(contentId), any());
    verify(interactionRepository).save(any(UserContentInteraction.class));
  }

  @Test
  void 기존_중복_반응은_최신_한_건만_남긴다() {
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    UserContentInteraction latest = mock(UserContentInteraction.class);
    UserContentInteraction duplicate = mock(UserContentInteraction.class);
    when(latest.getType()).thenReturn(InteractionType.LIKE);
    when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(mock(User.class)));
    when(interactionRepository
        .findAllByUserIdAndContentIdAndTypeInOrderByUpdatedAtDescIdDesc(
            eq(userId), eq(contentId), any()))
        .thenReturn(List.of(latest, duplicate));

    reactionService.setReaction(userId, contentId, InteractionType.DISLIKE);

    verify(interactionRepository).deleteAllInBatch(List.of(duplicate));
    verify(latest).changeReactionType(InteractionType.DISLIKE);
  }
}
