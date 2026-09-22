package com.codeit.modoo_playlist.infra.repository;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

public interface RealtimeUserRepository extends Repository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID senderId);
}
