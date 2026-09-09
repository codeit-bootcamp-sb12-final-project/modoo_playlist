package com.codeit.modoo_playlist.moduleapi.domain.content.repository.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeit.modoo_playlist.core.domain.content.entity.ContentPerson;

public interface ContentPersonRepository extends JpaRepository<ContentPerson, UUID> {

    List<ContentPerson> findAllByContent_IdOrderByDisplayOrderAsc(UUID contentId);
}
