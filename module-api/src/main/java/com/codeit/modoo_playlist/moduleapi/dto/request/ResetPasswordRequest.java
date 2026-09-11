package com.codeit.modoo_playlist.moduleapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank @Email String email
) {

}
