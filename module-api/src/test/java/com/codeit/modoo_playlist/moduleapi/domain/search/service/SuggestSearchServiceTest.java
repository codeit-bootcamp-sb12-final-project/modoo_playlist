package com.codeit.modoo_playlist.moduleapi.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codeit.modoo_playlist.moduleapi.domain.search.document.ContentDocument;

@ExtendWith(MockitoExtension.class)
class SuggestSearchServiceTest {

  @Mock
  private ElasticsearchClient elasticsearchClient;

  @InjectMocks
  private SuggestSearchService service;

  private final List<SearchRequest> requests = new ArrayList<>();

  @Test
  void 유효하지_않은_입력은_조회하지_않는다() {
    assertThat(service.suggest(null)).isEmpty();
    assertThat(service.suggest("")).isEmpty();
    assertThat(service.suggest("   ")).isEmpty();
    assertThat(service.suggest("가".repeat(51))).isEmpty();

    verifyNoInteractions(elasticsearchClient);
  }

  @Test
  void 전체일치_앞부분일치_단어일치_순으로_반환한다() throws IOException {
    givenResults(
        List.of("변호사"),
        List.of("변호사들"),
        List.of("이상한 변호사 우영우")
    );

    assertThat(service.suggest("변호사"))
        .containsExactly("변호사", "변호사들", "이상한 변호사 우영우");

    assertThat(requests).hasSize(3);

    var exact = requests.get(0).query().term();
    assertThat(exact.field()).isEqualTo("normalizedTitle");
    assertThat(exact.value().stringValue()).isEqualTo("변호사");

    var prefix = requests.get(1).query().bool();
    assertThat(prefix.must().get(0).prefix().field()).isEqualTo("normalizedTitle");
    assertThat(prefix.must().get(0).prefix().value()).isEqualTo("변호사");
    assertThat(prefix.mustNot().get(0).term().value().stringValue()).isEqualTo("변호사");

    var expected = requests.get(2).query().bool();
    assertThat(expected.must().get(0).matchPhrasePrefix().field()).isEqualTo("title");
    assertThat(expected.must().get(0).matchPhrasePrefix().query()).isEqualTo("변호사");
    assertThat(expected.mustNot().get(0).prefix().value()).isEqualTo("변호사");

    for (SearchRequest request : requests) {
      assertThat(request.index()).containsExactly("contents");
      assertThat(request.size()).isEqualTo(10);
    }
  }

  @Test
  void 정규화한_검색어로_조회한다() throws IOException {
    givenResults(List.of(), List.of(), List.of());

    service.suggest("  ＢＡＴＣＨ   Test  ");

    assertThat(requests.get(0).query().term().value().stringValue())
        .isEqualTo("batch test");
    assertThat(requests.get(1).query().bool().must().get(0).prefix().value())
        .isEqualTo("batch test");
    assertThat(requests.get(2).query().bool().must().get(0).matchPhrasePrefix().query())
        .isEqualTo("batch test");
  }

  @Test
  void 길이는_정규화한_뒤_검증한다() throws IOException {
    givenResults(List.of(), List.of(), List.of());

    service.suggest(" ".repeat(30) + "변호사" + " ".repeat(30));

    assertThat(requests).hasSize(3);
    assertThat(requests.get(0).query().term().value().stringValue()).isEqualTo("변호사");
  }

  @Test
  void 후보가_10개면_다음_조회를_생략한다() throws IOException {
    List<String> titles = IntStream.rangeClosed(1, 10)
        .mapToObj(number -> "변호사 " + number)
        .toList();

    givenResults(List.of(), titles);

    assertThat(service.suggest("변호사")).containsExactlyElementsOf(titles);
    assertThat(requests).hasSize(2);
  }

  @Test
  void 합친_결과는_최대_10개를_반환한다() throws IOException {
    List<String> prefixes = IntStream.rangeClosed(1, 8)
        .mapToObj(number -> "변호사 " + number)
        .toList();

    givenResults(
        List.of("변호사"),
        prefixes,
        List.of("이상한 변호사 우영우", "나는 변호사다")
    );

    List<String> result = service.suggest("변호사");

    assertThat(result).hasSize(10);
    assertThat(result.get(0)).isEqualTo("변호사");
    assertThat(result.get(9)).isEqualTo("이상한 변호사 우영우");
    assertThat(result).doesNotContain("나는 변호사다");
  }

  @Test
  void 빈_제목과_중복을_제거한다() throws IOException {
    givenResults(
        Arrays.asList(null, " ", "변호사", "변호사"),
        List.of("변호사", "변호사들"),
        List.of("변호사들", "이상한 변호사 우영우")
    );

    assertThat(service.suggest("변호사")).containsExactly("변호사", "변호사들", "이상한 변호사 우영우");
  }

  @Test
  void 결과가_없으면_빈_목록을_반환한다() throws IOException {
    givenResults(List.of(), List.of(), List.of());

    assertThat(service.suggest("없는제목")).isEmpty();
    assertThat(requests).hasSize(3);
  }

  @Test
  void 통신_실패를_예외로_전달한다() throws IOException {
    IOException cause = new IOException("연결 실패");

    Mockito.when(elasticsearchClient.search(
        Mockito.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
        eq(ContentDocument.class)
    )).thenThrow(cause);

    assertThatThrownBy(() -> service.suggest("변호사"))
        .isInstanceOf(IllegalStateException.class)
        .hasCause(cause);
  }

  @SafeVarargs
  private final void givenResults(List<String>... results) throws IOException {
    Mockito.when(elasticsearchClient.search(
        Mockito.<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
        eq(ContentDocument.class)
    )).thenAnswer(invocation -> {
      Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> builder =
          invocation.getArgument(0);

      requests.add(builder.apply(new SearchRequest.Builder()).build());

      int index = requests.size() - 1;
      if (index >= results.length) {
        throw new AssertionError("예상보다 많은 ES 조회가 발생했습니다.");
      }

      return response(results[index]);
    });
  }

  private SearchResponse<ContentDocument> response(List<String> titles) {
    List<Hit<ContentDocument>> hits = IntStream.range(0, titles.size())
        .mapToObj(index -> Hit.of((Hit.Builder<ContentDocument> hit) -> hit
            .index("contents")
            .id("019ed8a0-0000-7000-9400-%012d".formatted(index + 1))
            .source(ContentDocument.builder().title(titles.get(index)).build())))
        .toList();

    return SearchResponse.of(response -> response
        .took(1)
        .timedOut(false)
        .shards(shards -> shards.total(1).successful(1).failed(0))
        .hits(metadata -> metadata.hits(hits)));
  }
}
