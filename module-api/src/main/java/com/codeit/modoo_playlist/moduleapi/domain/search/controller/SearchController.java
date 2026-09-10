package com.codeit.modoo_playlist.moduleapi.domain.search.controller;

import com.codeit.modoo_playlist.moduleapi.domain.search.service.PopularSearchService;
import com.codeit.modoo_playlist.moduleapi.dto.PopularKeywordDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

  private final PopularSearchService popularSearchService;

  // 인기 검색어 TOP 10 조회 (검색창 클릭/포커스 시 호출)
  @GetMapping("/popular")
  public ResponseEntity<List<PopularKeywordDto>> getPopularKeywords() {
    return ResponseEntity.ok(popularSearchService.getPopularKeywords());
  }

}
