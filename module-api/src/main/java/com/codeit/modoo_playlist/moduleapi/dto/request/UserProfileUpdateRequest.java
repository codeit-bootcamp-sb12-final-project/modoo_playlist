package com.codeit.modoo_playlist.moduleapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
    @NotBlank @Size(max = 50) String name
) {

}
