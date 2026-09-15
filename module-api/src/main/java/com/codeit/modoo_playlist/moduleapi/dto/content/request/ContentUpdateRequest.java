package com.codeit.modoo_playlist.moduleapi.dto.content.request;

import java.util.List;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

public record ContentUpdateRequest(
        @Size(max = 255)
        @Pattern(regexp = ".*\\S.*", message = "title은 공백일 수 없습니다.")
        String title,

        String description,

        LocalDate releaseDate,

        @Size(max = 20)
        String originCountry,

        List<@NotBlank @Size(max = 50) String> tags,

        @Valid ContentVideoRequest video,

        @Valid ContentSportsRequest sports,

        List<@NotNull @Valid ContentPersonRequest> people
) {
    public ContentUpdateRequest(String title, String description, List<String> tags) {
        this(title, description, null, null, tags, null, null, null);
    }
}
