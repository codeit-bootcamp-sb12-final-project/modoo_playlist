package com.codeit.modoo_playlist.moduleapi.domain.chat.tool.search.util;

public class SearchQueryTextBuilder {

  private SearchQueryTextBuilder() {
  }

  public static String build(String query) {
    return "task: search result | query: " + query;
  }
}
