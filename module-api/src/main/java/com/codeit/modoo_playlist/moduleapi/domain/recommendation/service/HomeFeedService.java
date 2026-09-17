package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.HomeFeedResponse;
import java.util.UUID;

public interface HomeFeedService {

  HomeFeedResponse getHomeFeed(UUID userId);
}
