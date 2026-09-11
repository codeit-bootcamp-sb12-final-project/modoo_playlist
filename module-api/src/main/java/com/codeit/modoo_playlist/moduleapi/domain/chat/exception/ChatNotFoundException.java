package com.codeit.modoo_playlist.moduleapi.domain.chat.exception;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

public class ChatNotFoundException extends ChatException {
	public ChatNotFoundException() {
		super(ErrorCode.CONVERSATION_NOT_FOUND);
	}
}
