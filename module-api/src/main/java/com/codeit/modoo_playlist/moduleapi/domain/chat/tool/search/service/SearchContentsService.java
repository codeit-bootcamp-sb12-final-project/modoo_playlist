package com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.service;

import com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.dto.SearchContentDto;
import java.util.List;

public interface SearchContentsService {
  List<SearchContentDto> search(String query, Integer limit);
}
