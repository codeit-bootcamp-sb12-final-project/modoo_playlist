package com.codeit.modoo_playlist.moduleapi.domain.search.repository;

import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;
import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ContentSearchRepository extends ElasticsearchRepository<ContentDocument, String> {

  Optional<ContentDocument> findFirstByNormalizedTitle(String normalizedTitle);

}
