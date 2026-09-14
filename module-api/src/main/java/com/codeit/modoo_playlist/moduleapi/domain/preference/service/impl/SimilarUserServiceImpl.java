package com.codeit.modoo_playlist.moduleapi.domain.preference.service.impl;

import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.SimilarUserDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.repository.UserSimilarityRepository;
import com.codeit.modoo_playlist.moduleapi.domain.preference.service.SimilarUserService;
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
public class SimilarUserServiceImpl implements SimilarUserService {

  private final UserSimilarityRepository userSimilarityRepository;


  @Override
  public List<SimilarUserDto> getMySimilarUsers(UUID id, Integer limit) {
    return userSimilarityRepository.findTopSimilarUsersByUserId(id, PageRequest.of(0, limit));
  }
}
