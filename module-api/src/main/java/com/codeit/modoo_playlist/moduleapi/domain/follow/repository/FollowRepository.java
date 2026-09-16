package com.codeit.modoo_playlist.moduleapi.domain.follow.repository;

import com.codeit.modoo_playlist.core.domain.follow.entity.Follow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    long countByFolloweeId(UUID followeeId);

    @Query("select f.followeeId from Follow f where f.followerId = :followerId")
    List<UUID> findFolloweeIdsByFollowerId(@Param("followerId") UUID followerId);

}