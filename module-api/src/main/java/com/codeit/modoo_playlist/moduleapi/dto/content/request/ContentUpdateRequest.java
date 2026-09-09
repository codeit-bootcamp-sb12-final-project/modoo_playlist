package com.codeit.modoo_playlist.moduleapi.dto.content.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContentUpdateRequest(
        @Size(max = 255)
        @Pattern(regexp = ".*\\S.*", message = "title은 공백일 수 없습니다.")
        String title,

        String description,

        List<@NotBlank @Size(max = 50) String> tags
) {
}
