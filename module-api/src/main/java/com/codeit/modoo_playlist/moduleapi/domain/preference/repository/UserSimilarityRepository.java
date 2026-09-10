package com.codeit.modoo_playlist.moduleapi.domain.preference.repository;

import com.codeit.modoo_playlist.core.domain.preference.entity.UserSimilarity;
import com.codeit.modoo_playlist.core.domain.preference.entity.UserSimilarityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSimilarityRepository extends JpaRepository<UserSimilarity, UserSimilarityId> {
}
