package com.codeit.modoo_playlist.moduleapi.domain.interaction.repository;

import com.codeit.modoo_playlist.core.domain.interaction.entity.UserContentInteraction;
import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarUserInteractionProjection;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserContentInteractionRepository extends
    JpaRepository<UserContentInteraction, UUID> {

  Optional<UserContentInteraction> findByUserIdAndContentIdAndTypeIn(UUID userId, UUID contentId,
      Collection<InteractionType> types);

  Optional<UserContentInteraction> findByUserIdAndContentIdAndType(UUID userId, UUID contentId,
      InteractionType type);

  @Query("""
      select new com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.SimilarUserInteractionProjection(
        uci.content.id, uci.content.title, uci.content.thumbnailUrl,
        uci.user.id, uci.type, uci.value, uci.occurrenceCount
      )
      from UserContentInteraction uci
      where uci.user.id in :similarUserIds
        and uci.content.deletedAt is null
        and uci.content.id not in (
          select uci2.content.id from UserContentInteraction uci2 where uci2.user.id = :myUserId
        )
      order by uci.content.id
      """)
  List<SimilarUserInteractionProjection> findCandidateInteractions(
      @Param("myUserId") UUID myUserId, @Param("similarUserIds") List<UUID> similarUserIds,
      Pageable pageable);
}
