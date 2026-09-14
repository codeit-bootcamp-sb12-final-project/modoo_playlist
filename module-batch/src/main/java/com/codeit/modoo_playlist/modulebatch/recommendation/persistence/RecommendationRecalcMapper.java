package com.codeit.modoo_playlist.modulebatch.recommendation.persistence;

import com.codeit.modoo_playlist.modulebatch.recommendation.model.InteractionTagSignal;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.PreferenceTagRow;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.SimilarityRow;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.TagContentCount;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.TagName;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.UserTagScore;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationRecalcMapper {

	List<InteractionTagSignal> findInteractionTagSignals();

	List<TagContentCount> findTagContentCounts();

	long countActiveContents();

	void upsertPreferenceTags(@Param("rows") List<PreferenceTagRow> rows);

	List<UserTagScore> findAllPreferenceScores();

	List<TagName> findTagNames();

	void upsertSimilarities(@Param("rows") List<SimilarityRow> rows);
}
