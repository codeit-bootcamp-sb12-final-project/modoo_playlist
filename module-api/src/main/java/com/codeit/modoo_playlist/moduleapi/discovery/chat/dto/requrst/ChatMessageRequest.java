package com.codeit.modoo_playlist.moduleapi.discovery.chat.dto.requrst;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ChatMessageRequest(
	UUID conversationId,
	@NotBlank @Size(max = 2000) String message
) {
}
