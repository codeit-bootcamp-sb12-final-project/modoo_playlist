package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.codeit.modoo_playlist.core.global.common.util.KeywordNormalizer;
import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuggestSearchService {

  private static final String INDEX_NAME = "contents";
  private static final int LIMIT = 10;

  private final ElasticsearchClient elasticsearchClient;

  public List<String> suggest(String rawKeyword) {
    String keyword = KeywordNormalizer.normalize(rawKeyword);

    if (!KeywordNormalizer.isValidLength(keyword)) {
      return List.of();
    }

    Query exactQuery = Query.of(q -> q.term(t -> t
        .field("normalizedTitle").value(keyword)));

    Query prefixQuery = Query.of(q -> q.prefix(p -> p
        .field("normalizedTitle").value(keyword)));

    Set<String> suggestions = new LinkedHashSet<>();

    suggestions.addAll(search(exactQuery));

    if (suggestions.size() < LIMIT) {
      Query query = Query.of(q -> q.bool(b -> b
          .must(prefixQuery)
          .mustNot(exactQuery)));

      suggestions.addAll(search(query));
    }

    if (suggestions.size() < LIMIT) {
      Query query = Query.of(q -> q.bool(b -> b
          .must(m -> m.matchPhrasePrefix(p -> p
              .field("title").query(keyword).maxExpansions(50)))
          .mustNot(prefixQuery)));

      suggestions.addAll(search(query));
    }

    return suggestions.stream().limit(LIMIT).toList();
  }

  private List<String> search(Query query) {
    try {
      var response = elasticsearchClient.search(request -> request
              .index(INDEX_NAME)
              .size(LIMIT)
              .query(query)
              .sort(s -> s.score(score -> score.order(SortOrder.Desc)))
              .sort(s -> s.field(field -> field
                  .field("id.keyword").order(SortOrder.Asc))),
          ContentDocument.class);

      return response.hits().hits().stream()
          .map(Hit::source)
          .filter(document -> document != null && document.getTitle() != null)
          .map(ContentDocument::getTitle)
          .map(String::strip)
          .filter(title -> !title.isEmpty())
          .distinct()
          .toList();
    } catch (IOException exception) {
      throw new IllegalStateException("검색어 후보 조회에 실패했습니다.", exception);
    }
  }
}
