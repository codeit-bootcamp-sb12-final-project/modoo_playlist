package com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentSports;

public interface ContentSportsRepository extends JpaRepository<ContentSports, UUID> {
}
