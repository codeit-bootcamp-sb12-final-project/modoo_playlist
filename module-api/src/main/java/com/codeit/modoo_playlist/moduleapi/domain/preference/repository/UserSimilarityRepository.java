package com.codeit.modoo_playlist.moduleapi.domain.preference.repository;

import com.codeit.modoo_playlist.core.domain.preference.entity.UserSimilarity;
import com.codeit.modoo_playlist.core.domain.preference.entity.UserSimilarityId;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.SimilarUserDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSimilarityRepository extends JpaRepository<UserSimilarity, UserSimilarityId> {

  @Query("""
    select new com.codeit.modoo_playlist.moduleapi.domain.preference.dto.SimilarUserDto(
      us.otherUser.id, us.otherUser.username, us.otherUser.profileImageUrl, us.score, us.sharedTags
    )
    from UserSimilarity us
    where us.user.id = :userId
    order by us.score desc
    """)
  List<SimilarUserDto> findTopSimilarUsersByUserId(@Param("userId") UUID userId, PageRequest pageable);
}
