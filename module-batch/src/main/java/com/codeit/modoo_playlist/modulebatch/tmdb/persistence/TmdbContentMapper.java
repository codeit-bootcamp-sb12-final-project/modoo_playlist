package com.codeit.modoo_playlist.modulebatch.tmdb.persistence;

import java.util.List;
import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.codeit.modoo_playlist.modulebatch.tmdb.model.ExistingTmdbContent;
import com.codeit.modoo_playlist.modulebatch.tmdb.model.TmdbSyncContent;

@Mapper
public interface TmdbContentMapper {

    ExistingTmdbContent findBySourceId(
            @Param("type") String type,
            @Param("sourceId") String sourceId
    );

    List<ExistingTmdbContent.Person> findPeopleByContentId(@Param("contentId") String contentId);

    List<ExistingTmdbContent.Tag> findTagsByContentId(@Param("contentId") String contentId);

    List<String> findExistingSourceIds(
            @Param("type") String type,
            @Param("sourceIds") List<String> sourceIds
    );

    List<String> findActiveMovieSourceIds(
            @Param("cutoffDate") LocalDate cutoffDate,
            @Param("today") LocalDate today
    );

    List<String> findActiveTvSourceIds();

    void upsertContent(TmdbSyncContent content);

    void upsertVideo(@Param("contentId") String contentId, @Param("video") TmdbSyncContent.Video video);

    void deletePeople(@Param("contentId") String contentId);

    void insertPeople(@Param("contentId") String contentId, @Param("people") List<TmdbSyncContent.Person> people);

    void insertTags(@Param("tags") List<TmdbSyncContent.Tag> tags);

    void deleteMissingOpenApiTags(
            @Param("contentId") String contentId,
            @Param("tags") List<TmdbSyncContent.Tag> tags
    );

    void upsertContentTags(
            @Param("contentId") String contentId,
            @Param("tags") List<TmdbSyncContent.Tag> tags
    );
}
