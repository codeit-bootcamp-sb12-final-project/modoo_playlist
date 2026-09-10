package com.codeit.modoo_playlist.moduleapi.discovery.chat.exception;

import com.codeit.modoo_playlist.core.global.exception.ErrorCode;

public class ChatAccessDeniedException extends ChatException {
	public ChatAccessDeniedException() {
		super(ErrorCode.CONVERSATION_ACCESS_DENIED);
	}
}
