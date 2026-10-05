package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
import com.codeit.modoo_playlist.moduleapi.domain.chat.repository.ContentTitleRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

/**
 * 툴 인자로 받은 콘텐츠 참조(ID 또는 제목)를 콘텐츠 ID로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class ContentRefResolver {

  private final ContentTitleRepository contentTitleRepository;

  public UUID resolve(String ref) {
    return resolve(ref, null);
  }

  /** 제목으로 찾았으면 그 콘텐츠의 카드도 남긴다. 검색을 거치지 않아 카드가 빠지는 것을 막는다. */
  public UUID resolve(String ref, ToolContext toolContext) {
    UUID id = ChatToolContext.parseUuid(ref);
    if (id != null || ref == null || ref.isBlank()) {
      return id;
    }
    String title = normalizeTitle(ref);
    if (title.isEmpty()) {
      return null;
    }
    List<ContentCardDto> matches = contentTitleRepository.findCardsByNormalizedTitle(title);
    if (matches.size() != 1) {
      return null;
    }
    ContentCardDto card = matches.get(0);
    ContentCardCollector collector = toolContext == null ? null : ChatToolContext.findCardCollector(toolContext);
    if (collector != null) {
      collector.add(card.contentId(), card.title(), card.thumbnailUrl());
    }
    return card.contentId();
  }

  public static String normalizeTitle(String title) {
    return title.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "");
  }
}
