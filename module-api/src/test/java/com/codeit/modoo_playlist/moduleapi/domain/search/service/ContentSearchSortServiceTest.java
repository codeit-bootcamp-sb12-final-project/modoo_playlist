package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentSearchSortServiceTest {

  private final ContentSearchSortService service = new ContentSearchSortService();

  @Test
  void 추천순은_점수와_ID로_정렬한다() {
    List<SortOptions> sorts = service.createSortOptions("recommended", "DESCENDING");

    assertThat(sorts).hasSize(2);
    assertThat(sorts.get(0).score().order()).isEqualTo(SortOrder.Desc);
    assertThat(sorts.get(1).field().field()).isEqualTo("id.keyword");
  }

  @Test
  void 콘텐츠_정렬_필드를_ES_필드로_변환한다() {
    assertThat(service.createSortOptions("watcherCount", "DESCENDING").get(0).field().field()).isEqualTo("watcherCount");
    assertThat(service.createSortOptions("createdAt", "DESCENDING").get(0).field().field()).isEqualTo("createdAt");
    assertThat(service.createSortOptions("rate", "DESCENDING").get(0).field().field()).isEqualTo("averageRating");
  }

  @Test
  void 정렬_방향을_ES_정렬_방향으로_변환한다() {
    assertThat(service.createSortOptions("watcherCount", "ASCENDING").get(0).field().order()).isEqualTo(SortOrder.Asc);
    assertThat(service.createSortOptions("watcherCount", "DESCENDING").get(0).field().order()).isEqualTo(SortOrder.Desc);
  }

  @Test
  void 정렬별_커서를_실제_타입으로_변환한다() {
    assertThat(service.parseCursor("2026-09-23T00:00:00Z", "createdAt")).isEqualTo("2026-09-23T00:00:00Z");
    assertThat(service.parseCursor("15", "watcherCount")).isEqualTo(15L);
    assertThat(service.parseCursor("4.5", "rate")).isEqualTo(4.5);
  }

  @Test
  void 잘못된_정렬값과_커서는_거부한다() {
    assertThatThrownBy(() -> service.createSortOptions("unknown", "DESCENDING")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.parseCursor("-1", "watcherCount")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.parseCursor("잘못된날짜", "createdAt")).isInstanceOf(IllegalArgumentException.class);
  }
}