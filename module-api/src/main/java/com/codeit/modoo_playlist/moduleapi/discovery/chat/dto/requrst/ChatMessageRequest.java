package com.codeit.modoo_playlist.moduleapi.discovery.chat.dto.requrst;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ChatMessageRequest(
	UUID conversationId,
	@NotBlank String message
) {
}
