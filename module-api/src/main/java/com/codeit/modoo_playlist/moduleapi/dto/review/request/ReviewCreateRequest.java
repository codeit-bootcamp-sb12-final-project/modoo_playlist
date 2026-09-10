package com.codeit.modoo_playlist.moduleapi.dto.review.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ReviewCreateRequest(
        @NotNull
        UUID contentId,

        @NotBlank
        String text,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("5.0")
        BigDecimal rating
) {
}