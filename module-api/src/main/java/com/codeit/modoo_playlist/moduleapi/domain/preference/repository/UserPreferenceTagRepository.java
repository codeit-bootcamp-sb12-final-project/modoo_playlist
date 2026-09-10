package com.codeit.modoo_playlist.moduleapi.domain.preference.repository;

import com.codeit.modoo_playlist.core.domain.preference.entity.UserPreferenceTag;
import com.codeit.modoo_playlist.core.domain.preference.entity.UserPreferenceTagId;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPreferenceTagRepository extends
    JpaRepository<UserPreferenceTag, UserPreferenceTagId> {

  @Query("""
      select new com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto(
      	t.id, t.name, t.kind, upt.score
      )
      from UserPreferenceTag upt
      join upt.tag t
      where upt.user.id = :userId
      order by upt.score desc
      """)
  List<UserPreferenceTagDto> findTopTagsByUserId(@Param("userId") UUID userId,
      PageRequest pageable);
}
