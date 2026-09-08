package com.codeit.modoo_playlist.core.domain.interaction.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeit.modoo_playlist.core.domain.interaction.entity.UserContentInteraction;

public interface UserContentInteractionRepository extends JpaRepository<UserContentInteraction, UUID> {
}
