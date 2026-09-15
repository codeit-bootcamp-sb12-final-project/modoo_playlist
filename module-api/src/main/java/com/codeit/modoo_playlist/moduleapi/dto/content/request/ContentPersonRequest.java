package com.codeit.modoo_playlist.moduleapi.dto.content.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record ContentPersonRequest(
        @Size(max = 20) String roleType,
        @NotBlank @Size(max = 100) String personName,
        @Size(max = 100) String characterName,
        @Size(max = 100) String personId,
        @Size(max = 500)
        @Pattern(regexp = "^$|https?://.+", message = "personImg는 http 또는 https URL이어야 합니다.")
        String personImg
) {
}
