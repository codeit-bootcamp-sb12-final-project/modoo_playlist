package com.codeit.modoo_playlist.moduleapi.domain.tag.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.codeit.modoo_playlist.core.domain.tag.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findAllByNameIn(Collection<String> names);

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO tags (id, name, kind, created_at)
            VALUES (UUID_TO_BIN(:id), :name, :kind, CURRENT_TIMESTAMP(6))
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") String id,
            @Param("name") String name,
            @Param("kind") String kind
    );
}
