package com.codeit.modoo_playlist.moduleapi.dto.playlist.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PlaylistContentAddRequest(
        @NotNull
        UUID contentId
) {
}