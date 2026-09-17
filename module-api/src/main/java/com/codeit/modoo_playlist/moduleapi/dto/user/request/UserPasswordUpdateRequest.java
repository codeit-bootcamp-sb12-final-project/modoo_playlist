package com.codeit.modoo_playlist.moduleapi.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(
    @NotBlank
    @Size(min = 8, max = 255)
    String password
) {

}