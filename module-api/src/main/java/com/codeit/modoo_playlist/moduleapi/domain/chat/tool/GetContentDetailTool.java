package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.moduleapi.domain.recommendation.dto.ContentDetailDto;
import com.codeit.modoo_playlist.moduleapi.domain.recommendation.service.ContentDetailService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetContentDetailTool {

  private final ContentDetailService contentDetailService;
  private final ContentRefResolver contentRefResolver;

  @Tool(
      name = "get_content_detail",
      description = "콘텐츠 하나의 상세 정보(유형·연도·국가·태그·감독·출연진·줄거리, 스포츠는 종목·리그·팀·경기 상태와 일시)를 조회합니다. "
          + "콘텐츠의 ID(UUID) 또는 정확한 제목이 필요하며, 이미 받은 결과에 그 값이 있으면 다시 호출하지 않습니다."
  )
  public List<ContentDetailDto> getContentDetail(
      @ToolParam(description = "조회할 콘텐츠의 ID(UUID) 또는 정확한 제목") String content,
      ToolContext toolContext
  ) {
    UUID id = contentRefResolver.resolve(content, toolContext);
    if (id == null) {
      log.info("get_content_detail 호출: ID 형식이 아니거나 제목과 맞는 콘텐츠가 하나가 아닙니다. value={}", content);
      return List.of();
    }
    log.info("get_content_detail 호출: contentId={}", id);

    List<ContentDetailDto> details = contentDetailService.getDetails(List.of(id));
    log.info("get_content_detail 결과: {}건", details.size());
    return details;
  }
}
