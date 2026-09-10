package com.codeit.modoo_playlist.moduleapi.domain.preference.service.impl;

import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.repository.UserPreferenceTagRepository;
import com.codeit.modoo_playlist.moduleapi.domain.preference.service.UserPreferenceTagService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPreferenceTagServiceImpl implements UserPreferenceTagService {

  private final UserPreferenceTagRepository userPreferenceTagRepository;

  @Override
  public List<UserPreferenceTagDto> getMyPreferenceTags(UUID id, Integer limit) {
    return userPreferenceTagRepository.findTopTagsByUserId(id, PageRequest.of(0, limit));
  }
}
