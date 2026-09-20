package com.codeit.modoo_playlist.moduleapi.domain.preference.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.SimilarUserDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.repository.UserSimilarityRepository;

@ExtendWith(MockitoExtension.class)
class SimilarUserServiceImplTest {

  @Mock private UserSimilarityRepository userSimilarityRepository;

  @Test
  void limit개를_요청하는_페이지로_유사_사용자_목록을_조회한다() {
    UUID userId = UUID.randomUUID();
    List<SimilarUserDto> expected = List.of(new SimilarUserDto(UUID.randomUUID(), "user2", null, null, null));
    when(userSimilarityRepository.findTopSimilarUsersByUserId(userId, PageRequest.of(0, 10)))
        .thenReturn(expected);

    List<SimilarUserDto> result = new SimilarUserServiceImpl(userSimilarityRepository)
        .getMySimilarUsers(userId, 10);

    assertThat(result).isEqualTo(expected);
  }
}
