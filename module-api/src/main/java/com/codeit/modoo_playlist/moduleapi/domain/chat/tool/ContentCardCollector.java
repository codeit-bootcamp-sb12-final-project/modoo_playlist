package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ContentCardCollector {

  private final Set<UUID> seenContentIds = ConcurrentHashMap.newKeySet();
  private final Queue<ContentCardDto> cards = new ConcurrentLinkedQueue<>();

  public void add(UUID contentId, String title, String thumbnailUrl) {
    if (seenContentIds.add(contentId)) {
      cards.add(new ContentCardDto(contentId, title, thumbnailUrl));
    }
  }

  public List<ContentCardDto> getCards() {
    return List.copyOf(cards);
  }
}
