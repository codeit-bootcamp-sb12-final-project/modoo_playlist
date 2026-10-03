package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.ContentDetailService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetContentDetailTool {

  private final ContentDetailService contentDetailService;

  @Tool(
      name = "get_content_detail",
      description = "이미 특정된 콘텐츠 하나의 상세 정보(유형·연도·국가·태그·감독·출연진·줄거리, 스포츠는 종목·리그·팀·경기 상태와 일시)를 조회합니다. "
          + "후속 질문('주연이 누구야?', '몇 년도 작품이야?')인데 이미 받은 결과에 그 값이 없을 때만 쓰며, 콘텐츠 ID가 필요합니다."
  )
  public List<ContentDetailDto> getContentDetail(
      @ToolParam(description = "조회할 콘텐츠 ID (UUID)") String contentId
  ) {
    UUID id = ChatToolContext.parseUuid(contentId);
    if (id == null) {
      log.warn("get_content_detail 호출: contentId가 없거나 형식이 올바르지 않습니다. value={}", contentId);
      return List.of();
    }
    log.info("get_content_detail 호출: contentId={}", id);

    List<ContentDetailDto> details = contentDetailService.getDetails(List.of(id));
    log.info("get_content_detail 결과: {}건", details.size());
    return details;
  }
}
