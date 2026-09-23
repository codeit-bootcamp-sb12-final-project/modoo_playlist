package com.codeit.modoo_playlist.moduleapi.domain.search.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codeit.modoo_playlist.core.domain.user.entity.UserRole;
import com.codeit.modoo_playlist.moduleapi.domain.content.controller.ContentController;
import com.codeit.modoo_playlist.moduleapi.domain.content.service.ContentService;
import com.codeit.modoo_playlist.moduleapi.domain.search.service.ContentSearchService;
import com.codeit.modoo_playlist.moduleapi.domain.search.service.PopularSearchService;
import com.codeit.modoo_playlist.moduleapi.domain.search.service.SuggestSearchService;
import com.codeit.modoo_playlist.moduleapi.dto.PopularKeywordDto;
import com.codeit.modoo_playlist.moduleapi.dto.UserDto;
import com.codeit.modoo_playlist.moduleapi.dto.content.request.ContentListRequest;
import com.codeit.modoo_playlist.moduleapi.dto.content.response.ContentCursorResponse;
import com.codeit.modoo_playlist.moduleapi.security.UserDetails;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class SearchControllerTest {

  private static final UUID USER_ID = UUID.fromString("019ed8a0-0000-7000-8000-000000000001");

  @Mock
  private ContentService contentService;

  @Mock
  private ContentSearchService contentSearchService;

  @Mock
  private PopularSearchService popularSearchService;

  @Mock
  private SuggestSearchService suggestSearchService;

  @InjectMocks
  private SearchController searchController;

  @InjectMocks
  private ContentController controller;

  @Test
  void 검색어가_있으면_검색_서비스의_결과를_반환한다() {
    ContentListRequest request = request("우주");
    ContentCursorResponse expected = emptyResponse();

    when(contentSearchService.searchPage(request)).thenReturn(expected);

    ResponseEntity<ContentCursorResponse> response = controller.getContents(request, user());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(expected);

    verify(contentSearchService).searchPage(request);
    verifyNoInteractions(contentService);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t\n", "\u3000"})
  void 검색어가_없거나_공백이면_기존_목록을_조회한다(String keyword) {
    ContentListRequest request = request(keyword);
    ContentCursorResponse expected = emptyResponse();

    when(contentService.getContents(request, USER_ID)).thenReturn(expected);

    ResponseEntity<ContentCursorResponse> response = controller.getContents(request, user());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(expected);

    verify(contentService).getContents(request, USER_ID);
    verifyNoInteractions(contentSearchService);
  }

  @Test
  void 인기검색어를_반환한다() {
    List<PopularKeywordDto> expected = List.of(
        new PopularKeywordDto("인터스텔라", 3L),
        new PopularKeywordDto("기생충", 2L)
    );
    when(popularSearchService.getPopularKeywords()).thenReturn(expected);

    ResponseEntity<List<PopularKeywordDto>> response = searchController.getPopularKeywords();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(expected);
    verify(popularSearchService).getPopularKeywords();
    verifyNoInteractions(suggestSearchService);
  }

  @Test
  void 자동완성_목록을_반환한다() {
    String keyword = "인터";
    List<String> expected = List.of("인터스텔라", "인터뷰");
    when(suggestSearchService.suggest(keyword)).thenReturn(expected);

    ResponseEntity<List<String>> response = searchController.getSuggestions(keyword);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(expected);
    verify(suggestSearchService).suggest(keyword);
    verifyNoInteractions(popularSearchService);
  }

  @Test
  void 검색어를_인기검색어_집계에_반영한다() {
    String keyword = "인터스텔라";

    ResponseEntity<Void> response = searchController.recordSearch(keyword);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
    verify(popularSearchService).recordSearch(keyword);
    verifyNoInteractions(suggestSearchService);
  }

  private ContentListRequest request(String keyword) {
    return new ContentListRequest(
        null,
        keyword,
        null,
        null,
        null,
        20,
        "DESCENDING",
        "watcherCount"
    );
  }

  private ContentCursorResponse emptyResponse() {
    return new ContentCursorResponse(
        List.of(),
        null,
        null,
        false,
        0,
        "watcherCount",
        "DESCENDING"
    );
  }

  private UserDetails user() {
    return new UserDetails(
        new UserDto(
            USER_ID,
            "user@test.com",
            "user",
            null,
            UserRole.USER,
            false,
            null
        ),
        "password"
    );
  }

}
