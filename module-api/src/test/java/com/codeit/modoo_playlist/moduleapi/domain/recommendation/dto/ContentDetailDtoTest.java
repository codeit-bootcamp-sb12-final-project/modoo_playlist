package com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;

import com.codeit.modoo_playlist.core.domain.content.type.ContentType;

class ContentDetailDtoTest {

  @Test
  void 제목만_있는_결과는_비어있는_필드를_JSON에서_생략한다() {
    UUID id = UUID.randomUUID();

    String json = toolResultJson(ContentDetailDto.titleOnly(id, "제목"));

    assertThat(json).contains("\"title\":\"제목\"").contains(id.toString());
    assertThat(json).doesNotContain("releaseYear", "tags", "directors", "actors", "sports", "description");
  }

  @Test
  void 값이_있는_필드는_JSON에_포함하고_없는_필드는_생략한다() {
    ContentDetailDto detail = new ContentDetailDto(
        UUID.randomUUID(), "기생충", ContentType.MOVIE, 2019, "KR",
        List.of("스릴러"), List.of("봉준호"), List.of(), "설명", null);

    String json = toolResultJson(detail);

    assertThat(json).contains("\"releaseYear\":2019", "\"originCountry\":\"KR\"", "스릴러", "봉준호", "MOVIE");
    assertThat(json).doesNotContain("actors", "sports");
  }

  private String toolResultJson(ContentDetailDto detail) {
    return new DefaultToolCallResultConverter().convert(detail, ContentDetailDto.class);
  }
}
