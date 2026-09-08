package com.codeit.modoo_playlist.core.domain.preference.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeit.modoo_playlist.core.domain.preference.entity.UserSimilarity;
import com.codeit.modoo_playlist.core.domain.preference.entity.UserSimilarityId;

public interface UserSimilarityRepository extends JpaRepository<UserSimilarity, UserSimilarityId> {
}
