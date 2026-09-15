package com.codeit.modoo_playlist.moduleapi.dto.content.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.time.Instant;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ContentRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void 생성요청은_지원하는_타입과_제목이_필수다() {
        ContentCreateRequest request = new ContentCreateRequest("book", " ", null, List.of());
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("type", "title");
    }

    @Test
    void 생성요청의_태그_요소도_검증한다() {
        ContentCreateRequest request = new ContentCreateRequest(
                "movie", "제목", null, List.of(" ", "a".repeat(51))
        );
        assertThat(validator.validate(request)).hasSize(2);
    }

    @Test
    void 목록요청의_limit과_정렬값을_검증한다() {
        ContentListRequest request = new ContentListRequest(
                "book", null, null, null, null, 101, "DOWN", "unknown"
        );
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("typeEqual", "limit", "sortDirection", "sortBy");
    }

    @Test
    void 수정요청은_공백_제목을_거부하지만_null은_허용한다() {
        assertThat(validator.validate(new ContentUpdateRequest(" ", null, null))).hasSize(1);
        assertThat(validator.validate(new ContentUpdateRequest(null, null, null))).isEmpty();
    }

    @Test
    void 등장인물_사진은_http_URL만_허용한다() {
        ContentPersonRequest person = new ContentPersonRequest(
                "CAST", "배우", null, null, "javascript:alert(1)"
        );

        assertThat(validator.validate(person))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("personImg");
    }

    @Test
    void 생성과_수정요청은_null_등장인물_요소를_거부한다() {
        ContentCreateRequest createRequest = new ContentCreateRequest(
                "movie", "제목", null, null, null, List.of(), null, null,
                java.util.Collections.singletonList(null)
        );
        ContentUpdateRequest updateRequest = new ContentUpdateRequest(
                null, null, null, null, null, null, null,
                java.util.Collections.singletonList(null)
        );

        assertThat(validator.validate(createRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("people[0].<list element>");
        assertThat(validator.validate(updateRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("people[0].<list element>");
    }

    @Test
    void 외부평점은_소수_첫째자리까지만_허용한다() {
        ContentVideoRequest video = new ContentVideoRequest(
                null, null, null, null, null, null, null, null,
                new java.math.BigDecimal("8.25"), null
        );

        assertThat(validator.validate(video))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("externalRating");
    }

    @Test
    void 스포츠_요청의_문자열_길이와_중첩_검증이_적용된다() {
        ContentCreateRequest request = new ContentCreateRequest(
                "sport", "경기", null, null, null, List.of(), null,
                new ContentSportsRequest("a".repeat(51), "리그", null,
                        "홈", "원정", null, null, Instant.now()),
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("sports.sportType");
    }
}
