package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ContentCardCollector {

  private final Set<UUID> seenContentIds = ConcurrentHashMap.newKeySet();
  private final Queue<ContentCardDto> cards = new ConcurrentLinkedQueue<>();
  private final AtomicInteger retries = new AtomicInteger();

  public void add(UUID contentId, String title, String thumbnailUrl) {
    if (seenContentIds.add(contentId)) {
      cards.add(new ContentCardDto(contentId, title, thumbnailUrl));
    }
  }

  public List<ContentCardDto> getCards() {
    return List.copyOf(cards);
  }

  // 이번 답변에서 몇 번째 search_contents 재검색인지
  public int nextRetry() {
    return retries.incrementAndGet();
  }

  // 재검색 시 버려진 이전 검색 결과의 카드를 지운다
  public void clear() {
    seenContentIds.clear();
    cards.clear();
  }
}
