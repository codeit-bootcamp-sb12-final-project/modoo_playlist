package com.codeit.modoo_playlist.moduleapi.interaction.service;

import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import java.util.UUID;

public interface ReactionService {

  void setReaction(UUID userId, UUID contentId, InteractionType type);
}
