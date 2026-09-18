package com.codeit.modoo_playlist.moduleapi.domain.search.controller;

import com.codeit.modoo_playlist.moduleapi.domain.search.service.PopularSearchService;
import com.codeit.modoo_playlist.moduleapi.domain.search.service.SuggestSearchService;
import com.codeit.modoo_playlist.moduleapi.dto.PopularKeywordDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

  private final PopularSearchService popularSearchService;
  private final SuggestSearchService suggestSearchService;

  @GetMapping("/popular")
  public ResponseEntity<List<PopularKeywordDto>> getPopularKeywords() {
    return ResponseEntity.ok(popularSearchService.getPopularKeywords());
  }

  @GetMapping("/suggestions")
  public ResponseEntity<List<String>> getSuggestions(
      @RequestParam(name = "keyword", defaultValue = "") String keyword) {
    return ResponseEntity.ok(suggestSearchService.suggest(keyword));
  }

}
