package com.codeit.modoo_playlist.moduleapi.domain.preference.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.codeit.modoo_playlist.core.domain.tag.type.TagKind;
import com.codeit.modoo_playlist.moduleapi.domain.preference.dto.UserPreferenceTagDto;
import com.codeit.modoo_playlist.moduleapi.domain.preference.repository.UserPreferenceTagRepository;

@ExtendWith(MockitoExtension.class)
class UserPreferenceTagServiceImplTest {

  @Mock private UserPreferenceTagRepository userPreferenceTagRepository;

  @Test
  void limit개를_요청하는_페이지로_취향_태그_목록을_조회한다() {
    UUID userId = UUID.randomUUID();
    List<UserPreferenceTagDto> expected = List.of(
        new UserPreferenceTagDto(UUID.randomUUID(), "액션", TagKind.GENRE, new BigDecimal("1.0")));
    when(userPreferenceTagRepository.findTopTagsByUserId(userId, PageRequest.of(0, 5)))
        .thenReturn(expected);

    List<UserPreferenceTagDto> result = new UserPreferenceTagServiceImpl(userPreferenceTagRepository)
        .getMyPreferenceTags(userId, 5);

    assertThat(result).isEqualTo(expected);
  }
}
