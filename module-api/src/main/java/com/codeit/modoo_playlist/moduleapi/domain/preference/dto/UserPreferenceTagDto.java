package com.codeit.modoo_playlist.moduleapi.domain.preference.dto;

import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import java.math.BigDecimal;
import java.util.UUID;

public record UserPreferenceTagDto(
		UUID tagId,
		String tagName,
		TagKind tagKind,
		BigDecimal score
) {
}
