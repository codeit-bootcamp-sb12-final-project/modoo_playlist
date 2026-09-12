package com.codeit.modoo_playlist.moduleapi.domain.user.repository;

import com.codeit.modoo_playlist.core.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

//  기존 jwt UserDetails가 username으로 조회하는데, 이메일 조회로 변경할지는 추후 결정. 아마 바꿀듯?
  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :userId")
  Optional<User> findByIdForUpdate(@Param("userId") UUID userId);

}
