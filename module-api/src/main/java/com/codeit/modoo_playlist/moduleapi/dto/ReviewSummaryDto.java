package com.codeit.modoo_playlist.moduleapi.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewSummaryDto(
    UUID contentId,
    String summary,
    Instant updatedAt
) {

}
