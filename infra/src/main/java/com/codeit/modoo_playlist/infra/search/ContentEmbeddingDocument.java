package com.codeit.modoo_playlist.infra.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;

@Document(indexName = ContentEmbeddingDocument.INDEX_NAME, createIndex = false)
@Mapping(mappingPath = "elasticsearch/content-embedding-mapping.json")
public record ContentEmbeddingDocument(
    @Id String contentId,
    float[] vector,
    String title,
    String thumbnailUrl
) {

  public static final String INDEX_NAME = "content-embeddings";
  public static final String VECTOR_FIELD = "vector";
}
