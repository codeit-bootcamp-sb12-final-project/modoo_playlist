package com.codeit.modoo_playlist.moduleapi.domain.recommendation.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchQueryTextBuilderTest {

  @Test
  void 비대칭_검색용_질의_포맷으로_감싼다() {
    assertThat(SearchQueryTextBuilder.build("우울할 때 볼만한 영화"))
        .isEqualTo("task: search result | query: 우울할 때 볼만한 영화");
  }
}
