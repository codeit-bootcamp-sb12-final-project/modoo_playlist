package com.codeit.modoo_playlist.moduleapi.domain.recommendation.service;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import java.util.List;
import java.util.UUID;

public interface ContentDetailService {

  List<ContentDetailDto> getDetails(List<UUID> contentIds);
}
