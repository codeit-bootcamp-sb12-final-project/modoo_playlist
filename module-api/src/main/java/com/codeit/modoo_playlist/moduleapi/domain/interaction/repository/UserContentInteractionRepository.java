package com.codeit.modoo_playlist.moduleapi.domain.interaction.repository;

import com.codeit.modoo_playlist.core.domain.interaction.entity.UserContentInteraction;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContentInteractionRepository extends JpaRepository<UserContentInteraction, UUID> {
}
