package com.codeit.modoo_playlist.moduleapi.domain.chat.dto.response;

import java.util.List;

public record ChatCardsEvent(
    List<ContentCardDto> cards
) {
}
