package com.codeit.modoo_playlist.moduleapi.domain.preference.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SimilarUserDto(
    UUID userId,
    String username,
    String profileImageUrl,
    BigDecimal score,
    String sharedTags
) {

}
