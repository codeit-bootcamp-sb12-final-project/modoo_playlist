package com.codeit.modoo_playlist.moduleapi.dto.content.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
}
