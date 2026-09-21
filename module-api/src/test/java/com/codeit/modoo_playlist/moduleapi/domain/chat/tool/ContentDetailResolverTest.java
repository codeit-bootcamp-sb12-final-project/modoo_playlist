package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.RecommendedContentDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.ContentDetailService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentDetailResolverTest {

  @Mock private ContentDetailService contentDetailService;

  @Test
  void 검색결과의_ID로_상세정보를_조회해_반환한다() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    List<ContentDetailDto> details = List.of(
        ContentDetailDto.titleOnly(first, "첫째"), ContentDetailDto.titleOnly(second, "둘째"));
    when(contentDetailService.getDetails(List.of(first, second))).thenReturn(details);

    List<ContentDetailDto> result = new ContentDetailResolver(contentDetailService).resolve(List.of(
        new RecommendedContentDto(first, "첫째", "thumb1", 0.9),
        new RecommendedContentDto(second, "둘째", "thumb2", 0.8)));

    assertThat(result).isEqualTo(details);
  }

  @Test
  void 상세_조회가_실패하면_제목만_담아_순서대로_반환한다() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    when(contentDetailService.getDetails(List.of(first, second))).thenThrow(new RuntimeException("DB 장애"));

    List<ContentDetailDto> result = new ContentDetailResolver(contentDetailService).resolve(List.of(
        new RecommendedContentDto(first, "첫째", "thumb1", 0.9),
        new RecommendedContentDto(second, "둘째", "thumb2", 0.8)));

    assertThat(result).containsExactly(
        ContentDetailDto.titleOnly(first, "첫째"), ContentDetailDto.titleOnly(second, "둘째"));
  }

  @Test
  void 결과가_없으면_빈_리스트를_반환한다() {
    when(contentDetailService.getDetails(List.of())).thenReturn(List.of());

    assertThat(new ContentDetailResolver(contentDetailService).resolve(List.of())).isEmpty();
  }
}
