package com.codeit.modoo_playlist.moduleapi.dto.review.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReviewUpdateRequest(
        @NotBlank
        String text,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("9.9")
        BigDecimal rating
) {
}