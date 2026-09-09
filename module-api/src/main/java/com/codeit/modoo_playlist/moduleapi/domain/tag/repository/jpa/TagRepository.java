package com.codeit.modoo_playlist.moduleapi.domain.tag.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findAllByNameIn(Collection<String> names);
}
