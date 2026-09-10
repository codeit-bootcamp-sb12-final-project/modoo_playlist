package com.codeit.modoo_playlist.moduleapi.domain.preference.service;

import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;
import java.util.List;
import java.util.UUID;

public interface UserPreferenceTagService {

  List<UserPreferenceTagDto> getMyPreferenceTags(UUID id, Integer limit);
}
