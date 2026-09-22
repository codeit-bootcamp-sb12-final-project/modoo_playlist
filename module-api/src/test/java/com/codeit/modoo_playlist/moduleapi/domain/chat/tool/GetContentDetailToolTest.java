package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.ContentDetailService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetContentDetailToolTest {

  @Mock private ContentDetailService contentDetailService;

  private GetContentDetailTool tool() {
    return new GetContentDetailTool(contentDetailService);
  }

  @Test
  void contentId가_없거나_형식이_잘못되면_빈_리스트를_반환한다() {
    assertThat(tool().getContentDetail(null)).isEmpty();
    assertThat(tool().getContentDetail("uuid-아님")).isEmpty();
    verifyNoInteractions(contentDetailService);
  }

  @Test
  void 상세정보가_있으면_그대로_반환한다() {
    UUID contentId = UUID.randomUUID();
    ContentDetailDto detail = ContentDetailDto.titleOnly(contentId, "제목");
    when(contentDetailService.getDetails(List.of(contentId))).thenReturn(List.of(detail));

    assertThat(tool().getContentDetail(contentId.toString())).containsExactly(detail);
  }

  @Test
  void 콘텐츠가_없거나_삭제됐으면_빈_리스트를_반환한다() {
    UUID contentId = UUID.randomUUID();
    when(contentDetailService.getDetails(List.of(contentId))).thenReturn(List.of());

    assertThat(tool().getContentDetail(contentId.toString())).isEmpty();
  }

  @Test
  void 조회_중_예외가_나면_삼키지_않고_전파한다() {
    UUID contentId = UUID.randomUUID();
    when(contentDetailService.getDetails(List.of(contentId))).thenThrow(new RuntimeException("DB 장애"));

    assertThatThrownBy(() -> tool().getContentDetail(contentId.toString())).hasMessage("DB 장애");
  }
}
