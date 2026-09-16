package com.codeit.modoo_playlist.moduleapi.domain.recommendation.util;

public class SearchQueryTextBuilder {

  private SearchQueryTextBuilder() {
  }

  public static String build(String query) {
    return "task: search result | query: " + query;
  }
}
