package com.codeit.modoo_playlist.moduleapi.domain.preference.repository;

import com.codeit.modoo_playlist.core.domain.preference.entity.UserPreferenceTag;
import com.codeit.modoo_playlist.core.domain.preference.entity.UserPreferenceTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferenceTagRepository extends JpaRepository<UserPreferenceTag, UserPreferenceTagId> {
}
