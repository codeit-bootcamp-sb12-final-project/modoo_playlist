package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import com.codeit.modoo_playlist.moduleapi.domain.search.event.SearchExecutedEvent;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.ContentSearchRepository;
import com.codeit.modoo_playlist.moduleapi.domain.search.repository.PopularSearchRedisRepository;
import com.codeit.modoo_playlist.moduleapi.dto.PopularKeywordDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
public class PopularSearchServiceTest {

  @Mock
  private PopularSearchRedisRepository popularSearchRedisRepository;

  @Mock
  private ContentSearchRepository contentSearchRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private PopularSearchService popularSearchService;

  @Test
  void 인기_검색어를_조회한다() {
    PopularKeywordDto keyword = mock(PopularKeywordDto.class);
    when(popularSearchRedisRepository.getTopKeywords(7, 10)).thenReturn(List.of(keyword));

    assertThat(popularSearchService.getPopularKeywords()).containsExactly(keyword);

    verify(popularSearchRedisRepository).getTopKeywords(7, 10);
  }

  @Test
  void 두글자_이상_검색어는_이벤트를_발행한다() {
    String keyword = "영화";
    popularSearchService.recordSearch(keyword);
    verify(eventPublisher).publishEvent(any(SearchExecutedEvent.class));
  }

  @Test
  void 한글자_검색어가_콘텐츠_제목과_일치하면_이벤트를_발행한다() {
    String keyword = "봄";
    ContentDocument document = mock(ContentDocument.class);
    when(contentSearchRepository.findFirstByNormalizedTitle(keyword)).thenReturn(Optional.of(document));
    popularSearchService.recordSearch(keyword);
    verify(eventPublisher).publishEvent(any(SearchExecutedEvent.class));
  }

  @Test
  void 한글자_검색어가_콘텐츠_제목과_일치하지_않으면_이벤트를_발행하지_않는다() {
    String keyword = "봄";
    when(contentSearchRepository.findFirstByNormalizedTitle(keyword)).thenReturn(Optional.empty());
    popularSearchService.recordSearch(keyword);
    verify(eventPublisher, never()).publishEvent(any(SearchExecutedEvent.class));
  }

  @Test
  void 빈_검색어는_이벤트를_발행하지_않는다() {
    popularSearchService.recordSearch(" ");
    verifyNoInteractions(contentSearchRepository, eventPublisher);
  }

}
