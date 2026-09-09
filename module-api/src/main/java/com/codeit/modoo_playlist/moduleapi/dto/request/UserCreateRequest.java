package com.codeit.modoo_playlist.moduleapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @NotBlank @Email String email,

    @NotBlank @Size(max = 50) String name,

    @NotBlank @Size(max = 255) String password
) {

}
