package com.codeit.modoo_playlist.moduleapi.domain.interaction.repository;

import com.codeit.modoo_playlist.core.domain.interaction.entity.UserContentInteraction;
import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContentInteractionRepository extends JpaRepository<UserContentInteraction, UUID> {

  Optional<UserContentInteraction> findByUserIdAndContentIdAndTypeIn(UUID userId, UUID contentId, Collection<InteractionType> types);

  Optional<UserContentInteraction> findByUserIdAndContentIdAndType(UUID userId, UUID contentId, InteractionType type);
}
