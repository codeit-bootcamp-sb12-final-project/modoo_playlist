package com.codeit.modoo_playlist.modulebatch.embedding.processor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.codeit.modoo_playlist.modulebatch.embedding.model.ContentEmbeddingTarget;

class EmbeddingTextBuilderTest {

  @Test
  void 제목_설명_태그를_하나의_문서_포맷으로_합친다() {
    ContentEmbeddingTarget target = builder("영화")
        .description("우주를 배경으로 한 드라마").tagNames("SF,드라마").build();

    assertThat(EmbeddingTextBuilder.build(target))
        .isEqualTo("title: 영화 | text: 태그: SF, 드라마 | 줄거리: 우주를 배경으로 한 드라마");
  }

  @Test
  void 제목이_없으면_none으로_대체한다() {
    ContentEmbeddingTarget target = builder(null).description("설명").build();

    assertThat(EmbeddingTextBuilder.build(target)).startsWith("title: none | text:");
  }

  @Test
  void 본문에_쓸_값이_모두_없으면_본문은_비어있다() {
    ContentEmbeddingTarget target = builder("영화").build();

    assertThat(EmbeddingTextBuilder.build(target)).isEqualTo("title: 영화 | text: ");
  }

  @Test
  void 값이_없거나_공백인_항목은_라벨째로_생략한다() {
    ContentEmbeddingTarget target = builder("영화")
        .description("   ").tagNames(null).directors("").actors(" ").build();

    assertThat(EmbeddingTextBuilder.build(target)).isEqualTo("title: 영화 | text: ");
  }

  @Test
  void 영화의_모든_메타데이터를_정해진_순서로_합친다() {
    ContentEmbeddingTarget target = builder("기생충")
        .type("MOVIE").releaseYear(2019).originCountry("KR")
        .tagNames("블랙코미디,스릴러").directors("봉준호").actors("송강호,이선균")
        .description("반지하 가족의 이야기").build();

    assertThat(EmbeddingTextBuilder.build(target)).isEqualTo(
        "title: 기생충 | text: 유형: 영화 | 연도: 2019년 | 국가: 대한민국 (KR)"
            + " | 태그: 블랙코미디, 스릴러 | 감독: 봉준호 | 출연: 송강호, 이선균"
            + " | 줄거리: 반지하 가족의 이야기");
  }

  @Test
  void 콘텐츠_유형을_한글로_바꾸고_모르는_유형은_그대로_쓴다() {
    assertThat(EmbeddingTextBuilder.build(builder("a").type("MOVIE").build())).contains("유형: 영화");
    assertThat(EmbeddingTextBuilder.build(builder("a").type("TV").build())).contains("유형: TV 시리즈");
    assertThat(EmbeddingTextBuilder.build(builder("a").type("SPORT").build())).contains("유형: 스포츠");
    assertThat(EmbeddingTextBuilder.build(builder("a").type("ANIME").build())).contains("유형: ANIME");
  }

  @Test
  void 두글자_국가코드는_한글_이름과_코드를_함께_쓴다() {
    assertThat(EmbeddingTextBuilder.build(builder("a").originCountry("kr").build()))
        .contains("국가: 대한민국 (KR)");
    assertThat(EmbeddingTextBuilder.build(builder("a").originCountry("US").build()))
        .contains("국가: 미국 (US)");
  }

  @Test
  void 국가가_이미_이름이면_그대로_쓰고_해석_못하는_코드는_코드만_쓴다() {
    assertThat(EmbeddingTextBuilder.build(builder("a").originCountry("England").build()))
        .contains("국가: England");
    assertThat(EmbeddingTextBuilder.build(builder("a").originCountry("ZZ").build()))
        .contains("국가: ZZ");
  }

  @Test
  void 스포츠_정보를_종목_리그_시즌_경기_장소로_합친다() {
    ContentEmbeddingTarget target = builder("맨유 vs 아스날")
        .type("SPORT").releaseYear(2025).originCountry("England")
        .sportType("Soccer").league("Premier League").season("2025-2026")
        .homeTeam("Man Utd").awayTeam("Arsenal").venue("Old Trafford").build();

    assertThat(EmbeddingTextBuilder.build(target)).isEqualTo(
        "title: 맨유 vs 아스날 | text: 유형: 스포츠 | 연도: 2025년 | 국가: England"
            + " | 종목: Soccer | 리그: Premier League | 시즌: 2025-2026"
            + " | 경기: Man Utd vs Arsenal | 장소: Old Trafford");
  }

  @Test
  void 홈팀과_원정팀_중_하나만_있으면_있는_팀만_쓴다() {
    assertThat(EmbeddingTextBuilder.build(builder("a").homeTeam("Man Utd").build()))
        .contains("경기: Man Utd").doesNotContain("vs");
    assertThat(EmbeddingTextBuilder.build(builder("a").awayTeam("Arsenal").build()))
        .contains("경기: Arsenal").doesNotContain("vs");
  }

  @Test
  void 같은_입력이면_해시가_동일하다() {
    String hash1 = EmbeddingTextBuilder.hash("text", "model", "https://thumb");
    String hash2 = EmbeddingTextBuilder.hash("text", "model", "https://thumb");

    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  void 텍스트가_다르면_해시도_다르다() {
    String hash1 = EmbeddingTextBuilder.hash("text-a", "model", "https://thumb");
    String hash2 = EmbeddingTextBuilder.hash("text-b", "model", "https://thumb");

    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  void 썸네일이_null이면_빈문자열과_동일하게_취급한다() {
    String withNull = EmbeddingTextBuilder.hash("text", "model", null);
    String withEmpty = EmbeddingTextBuilder.hash("text", "model", "");

    assertThat(withNull).isEqualTo(withEmpty);
  }

  private ContentEmbeddingTarget.ContentEmbeddingTargetBuilder builder(String title) {
    return ContentEmbeddingTarget.builder()
        .contentId("content-id").title(title).thumbnailUrl("https://thumb");
  }
}
