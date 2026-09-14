package com.codeit.modoo_playlist.modulebatch.recommendation.persistence;

import com.codeit.modoo_playlist.modulebatch.recommendation.model.InteractionTagSignal;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.PreferenceTagRow;
import com.codeit.modoo_playlist.modulebatch.recommendation.model.TagContentCount;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationRecalcMapper {

	List<InteractionTagSignal> findInteractionTagSignals();

	List<TagContentCount> findTagContentCounts();

	long countActiveContents();

	void upsertPreferenceTags(@Param("rows") List<PreferenceTagRow> rows);
}
