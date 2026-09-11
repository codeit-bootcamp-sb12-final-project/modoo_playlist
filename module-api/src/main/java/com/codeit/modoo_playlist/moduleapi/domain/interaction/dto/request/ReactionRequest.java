package com.codeit.modoo_playlist.moduleapi.domain.interaction.dto.request;

import com.codeit.modoo_playlist.core.domain.interaction.enums.InteractionType;

public record ReactionRequest(
    InteractionType type
) {

}
