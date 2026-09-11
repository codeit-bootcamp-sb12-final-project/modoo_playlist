package com.codeit.modoo_playlist.modulebatch.sports.persistence;

import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.codeit.modoo_playlist.modulebatch.sports.model.ExistingSportsContent;
import com.codeit.modoo_playlist.modulebatch.sports.model.SportsSyncContent;

@Mapper
public interface SportsContentMapper {

    ExistingSportsContent findBySourceId(@Param("sourceId") String sourceId);

    String findContentIdBySourceId(@Param("sourceId") String sourceId);

    List<ExistingSportsContent.Tag> findTagsByContentId(@Param("contentId") String contentId);

    List<String> findRefreshSourceIds(
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd,
            @Param("now") Instant now,
            @Param("soccerDurationMinutes") int soccerDurationMinutes,
            @Param("basketballDurationMinutes") int basketballDurationMinutes,
            @Param("baseballDurationMinutes") int baseballDurationMinutes,
            @Param("defaultDurationMinutes") int defaultDurationMinutes
    );

    void upsertContent(SportsSyncContent content);

    void upsertSports(
            @Param("contentId") String contentId,
            @Param("sports") SportsSyncContent.Sports sports
    );

    void insertTags(@Param("tags") List<SportsSyncContent.Tag> tags);

    void deleteMissingOpenApiTags(
            @Param("contentId") String contentId,
            @Param("tags") List<SportsSyncContent.Tag> tags
    );

    void decreaseMissingOpenApiTagCounts(
            @Param("contentId") String contentId,
            @Param("tags") List<SportsSyncContent.Tag> tags
    );

    void increaseNewContentTagCounts(
            @Param("contentId") String contentId,
            @Param("tags") List<SportsSyncContent.Tag> tags
    );

    void upsertContentTags(
            @Param("contentId") String contentId,
            @Param("tags") List<SportsSyncContent.Tag> tags
    );

    int updateCalculatedStatuses(
            @Param("now") Instant now,
            @Param("soccerDurationMinutes") int soccerDurationMinutes,
            @Param("basketballDurationMinutes") int basketballDurationMinutes,
            @Param("baseballDurationMinutes") int baseballDurationMinutes,
            @Param("defaultDurationMinutes") int defaultDurationMinutes
    );
}
