package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentCardCollectorTest {

  @Test
  void 추가한_카드를_그대로_조회할_수_있다() {
    ContentCardCollector collector = new ContentCardCollector();
    UUID contentId = UUID.randomUUID();

    collector.add(contentId, "제목", "thumb");

    assertThat(collector.getCards()).containsExactly(new ContentCardDto(contentId, "제목", "thumb"));
  }

  @Test
  void 같은_contentId는_중복으로_담기지_않는다() {
    ContentCardCollector collector = new ContentCardCollector();
    UUID contentId = UUID.randomUUID();

    collector.add(contentId, "제목1", "thumb1");
    collector.add(contentId, "제목2", "thumb2");

    assertThat(collector.getCards()).hasSize(1);
    assertThat(collector.getCards().get(0).title()).isEqualTo("제목1");
  }

  @Test
  void 서로_다른_contentId는_둘_다_담긴다() {
    ContentCardCollector collector = new ContentCardCollector();

    collector.add(UUID.randomUUID(), "제목1", "thumb1");
    collector.add(UUID.randomUUID(), "제목2", "thumb2");

    assertThat(collector.getCards()).hasSize(2);
  }
}
