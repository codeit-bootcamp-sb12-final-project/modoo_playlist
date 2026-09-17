package com.codeit.modoo_playlist.moduleapi.domain.interaction.service.impl;

import com.codeit.modoo_playlist.core.domain.content.entity.Content;
import com.codeit.modoo_playlist.core.domain.interaction.entity.UserContentInteraction;
import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import com.codeit.modoo_playlist.core.domain.user.entity.User;
import com.codeit.modoo_playlist.core.global.exception.BaseException;
import com.codeit.modoo_playlist.core.global.exception.ErrorCode;
import com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa.ContentRepository;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.repository.UserContentInteractionRepository;
import com.codeit.modoo_playlist.moduleapi.domain.user.repository.UserRepository;
import com.codeit.modoo_playlist.moduleapi.domain.interaction.service.ReactionService;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReactionServiceImpl implements ReactionService {

  private static final Set<InteractionType> REACTION_TYPES =
      EnumSet.of(InteractionType.LIKE, InteractionType.DISLIKE, InteractionType.NOT_INTERESTED);

  private final UserContentInteractionRepository userContentInteractionRepository;
  private final UserRepository userRepository;
  private final ContentRepository contentRepository;

  @Override
  @Transactional
  public void setReaction(UUID userId, UUID contentId, InteractionType type) {
    if (!REACTION_TYPES.contains(type)) {
      throw new BaseException(ErrorCode.REACTION_TYPE_INVALID);
    }
    User user = userRepository.findByIdForUpdate(userId)
        .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    List<UserContentInteraction> reactions = userContentInteractionRepository
        .findAllByUserIdAndContentIdAndTypeInOrderByUpdatedAtDescIdDesc(
            userId, contentId, REACTION_TYPES);
    UserContentInteraction userContentInteraction = reactions.stream().findFirst().orElse(null);

    if (reactions.size() > 1) {
      userContentInteractionRepository.deleteAllInBatch(reactions.subList(1, reactions.size()));
    }

    if (userContentInteraction == null) {
      Content content = contentRepository.findById(contentId)
          .orElseThrow(() -> new BaseException(ErrorCode.CONTENT_NOT_FOUND));
      userContentInteractionRepository.save(
          UserContentInteraction.builder()
              .user(user)
              .content(content)
              .type(type)
              .build()
      );
    } else if (userContentInteraction.getType() != type) {
      userContentInteraction.changeReactionType(type);
    } else {
      userContentInteractionRepository.delete(userContentInteraction);
    }
  }
}
