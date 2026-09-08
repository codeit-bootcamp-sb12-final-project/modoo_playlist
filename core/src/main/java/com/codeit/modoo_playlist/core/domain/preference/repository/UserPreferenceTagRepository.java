package com.codeit.modoo_playlist.core.domain.preference.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeit.modoo_playlist.core.domain.preference.entity.UserPreferenceTag;
import com.codeit.modoo_playlist.core.domain.preference.entity.UserPreferenceTagId;

public interface UserPreferenceTagRepository extends JpaRepository<UserPreferenceTag, UserPreferenceTagId> {
}
