package com.codeit.modoo_playlist.moduleapi.domain.chat.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response.ContentCardDto;
import com.codeit.modoo_playlist.moduleapi.domain.chat.repository.ContentTitleRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

@ExtendWith(MockitoExtension.class)
class ContentRefResolverTest {

  @Mock private ContentTitleRepository contentTitleRepository;
  @InjectMocks private ContentRefResolver resolver;

  @Test
  void UUID_문자열은_저장소를_조회하지_않고_그대로_ID로_쓴다() {
    UUID id = UUID.randomUUID();

    assertThat(resolver.resolve(id.toString())).isEqualTo(id);
    verifyNoInteractions(contentTitleRepository);
  }

  @Test
  void 제목이_정확히_하나의_콘텐츠와_맞으면_그_ID를_돌려준다() {
    UUID id = UUID.randomUUID();
    when(contentTitleRepository.findCardsByNormalizedTitle("토이스토리5")).thenReturn(List.of(card(id)));

    assertThat(resolver.resolve("토이 스토리 5")).isEqualTo(id);
  }

  @Test
  void 제목은_공백과_대소문자를_무시하고_찾는다() {
    UUID id = UUID.randomUUID();
    when(contentTitleRepository.findCardsByNormalizedTitle("lesnovices")).thenReturn(List.of(card(id)));

    assertThat(resolver.resolve("  Les  Novices ")).isEqualTo(id);
  }

  @Test
  void 제목은_구두점을_무시하고_찾는다() {
    UUID id = UUID.randomUUID();
    when(contentTitleRepository.findCardsByNormalizedTitle("스파이더맨브랜드뉴데이")).thenReturn(List.of(card(id)));

    assertThat(resolver.resolve("스파이더맨: 브랜드 뉴 데이")).isEqualTo(id);
    assertThat(resolver.resolve("스파이더맨 - 브랜드 뉴 데이!")).isEqualTo(id);
  }

  @Test
  void 제목과_맞는_콘텐츠가_없으면_null이다() {
    when(contentTitleRepository.findCardsByNormalizedTitle("없는작품")).thenReturn(List.of());

    assertThat(resolver.resolve("없는 작품")).isNull();
  }

  @Test
  void 같은_제목이_여럿이면_고르지_않고_null이다() {
    when(contentTitleRepository.findCardsByNormalizedTitle("리메이크"))
        .thenReturn(List.of(card(UUID.randomUUID()), card(UUID.randomUUID())));

    assertThat(resolver.resolve("리메이크")).isNull();
  }

  @Test
  void 제목으로_찾으면_카드를_남기고_ID로_받으면_남기지_않는다() {
    UUID id = UUID.randomUUID();
    ContentCardCollector collector = new ContentCardCollector();
    ToolContext toolContext = new ToolContext(Map.of(ChatToolContext.CARD_COLLECTOR, collector));
    when(contentTitleRepository.findCardsByNormalizedTitle("토이스토리5")).thenReturn(List.of(card(id)));

    resolver.resolve(UUID.randomUUID().toString(), toolContext);
    assertThat(collector.getCards()).isEmpty();

    resolver.resolve("토이 스토리 5", toolContext);
    assertThat(collector.getCards()).containsExactly(card(id));
  }

  @Test
  void 비었거나_null이면_저장소를_조회하지_않고_null이다() {
    assertThat(resolver.resolve(null)).isNull();
    assertThat(resolver.resolve("   ")).isNull();
    assertThat(resolver.resolve("?!")).isNull();
    verifyNoInteractions(contentTitleRepository);
  }

  private static ContentCardDto card(UUID id) {
    return new ContentCardDto(id, "제목", "thumb");
  }
}
